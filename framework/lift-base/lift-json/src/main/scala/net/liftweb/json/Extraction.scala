/*
 * Copyright 2009-2010 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb {
package json {

import java.lang.reflect.{Constructor => JConstructor, Type}
import java.lang.{Integer => JavaInteger, Long => JavaLong, Short => JavaShort, Byte => JavaByte, Boolean => JavaBoolean, Double => JavaDouble, Float => JavaFloat}
import java.util.Date
import scala.reflect.Manifest
import JsonAST._

/** Function to extract values from JSON AST using case classes.
 *
 *  See: ExtractionExamples.scala
 */
object Extraction {
  import Meta._
  import Meta.Reflection._

  /** Extract a case class from JSON.
   * @see net.liftweb.json.JsonAST.JValue#extract
   * @throws MappingException is thrown if extraction fails
   */
  def extract[A](json: JValue)(implicit formats: Formats, mf: Manifest[A]): A = 
    try {
      extract0(json, formats, mf)
    } catch {
      case e: MappingException => throw e
      case e: Exception => throw new MappingException("unknown error", e)
    }

  /** Extract a case class from JSON.
   * @see net.liftweb.json.JsonAST.JValue#extract
   */
  def extractOpt[A](json: JValue)(implicit formats: Formats, mf: Manifest[A]): Option[A] = 
    try { Some(extract(json)(formats, mf)) } catch { case _: MappingException => None }

  /** Decompose a case class into JSON.
   * <p>
   * Example:<pre>
   * case class Person(name: String, age: Int)
   * implicit val formats = net.liftweb.json.DefaultFormats
   * Extraction.decompose(Person("joe", 25)) == JObject(JField("age",JInt(25)) :: JField("name",JString("joe")) :: Nil)
   * </pre>
   */
  def decompose(a: Any)(implicit formats: Formats): JValue = {
    def prependTypeHint(clazz: Class[_], o: JObject) = JField("jsonClass", JString(formats.typeHints.hintFor(clazz))) ++ o

    def mkObject(clazz: Class[_], fields: List[JField]) = formats.typeHints.containsHint_?(clazz) match {
      case true  => prependTypeHint(clazz, JObject(fields))
      case false => JObject(fields)
    }
 
    val serializer = formats.typeHints.serialize
    val any = a.asInstanceOf[AnyRef]
    if (formats.customSerializer(formats).isDefinedAt(a)) {
      formats.customSerializer(formats)(a)
    } else if (!serializer.isDefinedAt(a)) {
      any match {
        case null => JNull
        case x if primitive_?(x.getClass) => primitive2jvalue(x)(formats)
        case x: Map[_, _] => JObject((x map { case (k: String, v) => JField(k, decompose(v)) }).toList)
        case x: Collection[_] => JArray(x.toList map decompose)
        case x if (x.getClass.isArray) => JArray(x.asInstanceOf[Array[_]].toList map decompose)
        case x: Option[_] => x.flatMap[JValue] { y => Some(decompose(y)) }.getOrElse(JNothing)
        case x => 
          orderedConstructorArgs(x.getClass).map { f =>
            f.setAccessible(true)
            JField(unmangleName(f), decompose(f get x))
          } match {
            case fields => mkObject(x.getClass, fields)
          }
      }
    } else prependTypeHint(any.getClass, serializer(any))
  }
  
  /** Flattens the JSON to a key/value map.
   */
  def flatten(json: JValue): Map[String, String] = {
    def escapePath(str: String) = str

    def flatten0(path: String, json: JValue): Map[String, String] = {
      json match {
        case JNothing | JNull           => Map()
        case JString(s: String)         => Map(path -> ("\"" + quote(s) + "\""))
        case JDouble(num: Double)       => Map(path -> num.toString)
        case JInt(num: BigInt)          => Map(path -> num.toString)
        case JBool(value: Boolean)      => Map(path -> value.toString)
        case JField(name: String, 
                    value: JValue)      => flatten0(path + escapePath(name), value)
        case JObject(obj: List[JField]) => obj.foldLeft(Map[String, String]()) { (map, field) => map ++ flatten0(path + ".", field) }
        case JArray(arr: List[JValue])  => arr.length match {
          case 0 => Map(path -> "[]")
          case _ => arr.foldLeft((Map[String, String](), 0)) { 
                      (tuple, value) => (tuple._1 ++ flatten0(path + "[" + tuple._2 + "]", value), tuple._2 + 1) 
                    }._1
        }
      }
    }

    flatten0("", json)
  }

  /** Unflattens a key/value map to a JSON object.
   */
  def unflatten(map: Map[String, String]): JValue = {
    import scala.util.matching.Regex
    
    def extractValue(value: String): JValue = value.toLowerCase match {
      case ""      => JNothing
      case "null"  => JNull
      case "true"  => JBool(true)
      case "false" => JBool(false)
      case "[]"    => JArray(Nil)
      case x @ _   => 
        if (value.charAt(0).isDigit) {
          if (value.indexOf('.') == -1) JInt(BigInt(value)) else JDouble(value.toDouble)
        }
        else JString(JsonParser.unquote(value.substring(1)))
    }
  
    def submap(prefix: String): Map[String, String] = 
      Map(
        map.filter(t => t._1.startsWith(prefix)).map(
          t => (t._1.substring(prefix.length), t._2)
        ).toList.toArray: _*
      )
  
    val ArrayProp = new Regex("""^(\.([^\.\[]+))\[(\d+)\].*$""")
    val ArrayElem = new Regex("""^(\[(\d+)\]).*$""")
    val OtherProp = new Regex("""^(\.([^\.\[]+)).*$""")
  
    val uniquePaths = map.keys.foldLeft[Set[String]](Set()) { 
      (set, key) =>
        key match {
          case ArrayProp(p, f, i) => set + p
          case OtherProp(p, f)    => set + p    
          case ArrayElem(p, i)    => set + p        
          case x @ _              => set + x
        }
    }.toList.sort(_ < _) // Sort is necessary to get array order right
    
    uniquePaths.foldLeft[JValue](JNothing) { (jvalue, key) => 
      jvalue.merge(key match {
        case ArrayProp(p, f, i) => JObject(List(JField(f, unflatten(submap(key)))))
        case ArrayElem(p, i)    => JArray(List(unflatten(submap(key))))
        case OtherProp(p, f)    => JObject(List(JField(f, unflatten(submap(key)))))
        case ""                 => extractValue(map(key))
      })
    }
  }

  private def extract0[A](json: JValue, formats: Formats, mf: Manifest[A]): A = {
    if (mf.erasure == classOf[List[_]] || mf.erasure == classOf[Map[_, _]])
      fail("Root object can't yet be List or Map (needs a feature from Scala 2.8)")

    val mapping = mappingOf(mf.erasure)

    def newInstance(targetType: TypeInfo, args: List[Arg], json: JValue) = {
      def instantiate(constructor: JConstructor[_], args: List[Any]) = 
        try {
          if (constructor.getDeclaringClass == classOf[java.lang.Object]) fail("No information known about type")
          
          constructor.newInstance(args.map(_.asInstanceOf[AnyRef]).toArray: _*)
        } catch {
          case e @ (_:IllegalArgumentException | _:InstantiationException) =>             
            fail("Parsed JSON values do not match with class constructor\nargs=" + 
                 args.mkString(",") + "\narg types=" + args.map(a => if (a != null) 
                   a.asInstanceOf[AnyRef].getClass.getName else "null").mkString(",") + 
                 "\nconstructor=" + constructor)
        }

      def mkWithTypeHint(typeHint: String, fields: List[JField]) = {
        val obj = JObject(fields)
        val deserializer = formats.typeHints.deserialize
        if (!deserializer.isDefinedAt(typeHint, obj)) {
          val concreteClass = formats.typeHints.classFor(typeHint) getOrElse fail("Do not know how to deserialize '" + typeHint + "'")
          build(obj, mappingOf(concreteClass))
        } else deserializer(typeHint, obj)
      }

      val custom = formats.customDeserializer(formats)
      if (custom.isDefinedAt(targetType, json)) custom(targetType, json)
      else json match {
        case JObject(JField("jsonClass", JString(t)) :: xs) => mkWithTypeHint(t, xs)
        case JField(_, JObject(JField("jsonClass", JString(t)) :: xs)) => mkWithTypeHint(t, xs)
        case _ => instantiate(primaryConstructorOf(targetType.clazz), args.map(a => build(json \ a.path, a)))
      }
    }

    def newPrimitive(elementType: Class[_], elem: JValue) = convert(elem, elementType, formats)
    
    def newCollection(root: JValue, m: Mapping, constructor: Array[_] => Any) = {
      val array: Array[_] = root match {
        case JArray(arr)      => arr.map(build(_, m)).toArray
        case JNothing | JNull => Array[AnyRef]()
        case x                => fail("Expected collection but got " + x + " for root " + root + " and mapping " + m)
      }
      
      constructor(array)
    }

    def build(root: JValue, mapping: Mapping): Any = mapping match {
      case Value(targetType) => convert(root, targetType, formats)
      case Constructor(targetType, args) => root match {
        case JNull => null
        case _ => newInstance(targetType, args, root)
      }
      case Cycle(targetType) => build(root, mappingOf(targetType))
      case Arg(path, m) => mkValue(fieldValue(root), m, path)
      case Col(c, m) => {
        if (c == classOf[List[_]]) newCollection(root, m, a => List(a: _*))
        else if (c == classOf[Set[_]]) newCollection(root, m, a => Set(a: _*))
        else if (c.isArray) newCollection(root, m, mkTypedArray(c))
        else fail("Expected collection but got " + m + " for class " + c)
      }
      case Dict(m) => root match {
        case JObject(xs) => Map(xs.map(x => (x.name, build(x.value, m))): _*)
        case x => fail("Expected object but got " + x)
      }
      case Optional(m) =>
        // FIXME Remove this try-catch.
        try { 
          build(root, m) match {
            case null => None
            case x => Some(x)
          }
        } catch {
          case e: MappingException => None
        }
    }
    
    def mkTypedArray(c: Class[_])(a: Array[_]) = {
      import java.lang.reflect.Array.{newInstance => newArray}
      
      a.foldLeft((newArray(c.getComponentType, a.length), 0)) { (tuple, e) => {
        java.lang.reflect.Array.set(tuple._1, tuple._2, e); (tuple._1, tuple._2 + 1)
      }}._1
    }

    def mkList(root: JValue, m: Mapping) = root match {
      case JArray(arr) => arr.map(build(_, m))
      case JNothing | JNull => Nil
      case x => fail("Expected array but got " + x)
    }

    def mkValue(root: JValue, mapping: Mapping, path: String) = try {
      build(root, mapping)
    } catch { 
      case MappingException(msg, _) => fail("No usable value for " + path + "\n" + msg) 
    }

    def fieldValue(json: JValue): JValue = json match {
      case JField(_, value) => value
      case JNothing => JNothing
      case x => fail("Expected JField but got " + x)
    }

    build(json, mapping).asInstanceOf[A]
  }

  private def convert(json: JValue, targetType: Class[_], formats: Formats): Any = json match {
    case JInt(x) if (targetType == classOf[Int]) => x.intValue
    case JInt(x) if (targetType == classOf[JavaInteger]) => new JavaInteger(x.intValue)
    case JInt(x) if (targetType == classOf[BigInt]) => x
    case JInt(x) if (targetType == classOf[Long]) => x.longValue
    case JInt(x) if (targetType == classOf[JavaLong]) => new JavaLong(x.longValue)
    case JInt(x) if (targetType == classOf[Double]) => x.doubleValue
    case JInt(x) if (targetType == classOf[JavaDouble]) => new JavaDouble(x.doubleValue)
    case JInt(x) if (targetType == classOf[Float]) => x.floatValue
    case JInt(x) if (targetType == classOf[JavaFloat]) => new JavaFloat(x.floatValue)
    case JInt(x) if (targetType == classOf[Short]) => x.shortValue
    case JInt(x) if (targetType == classOf[JavaShort]) => new JavaShort(x.shortValue)
    case JInt(x) if (targetType == classOf[Byte]) => x.byteValue
    case JInt(x) if (targetType == classOf[JavaByte]) => new JavaByte(x.byteValue)
    case JInt(x) if (targetType == classOf[String]) => x.toString
    case JInt(x) if (targetType == classOf[Number]) => x.longValue
    case JDouble(x) if (targetType == classOf[Double]) => x
    case JDouble(x) if (targetType == classOf[JavaDouble]) => new JavaDouble(x)
    case JDouble(x) if (targetType == classOf[Float]) => x.floatValue
    case JDouble(x) if (targetType == classOf[JavaFloat]) => new JavaFloat(x.floatValue)
    case JDouble(x) if (targetType == classOf[String]) => x.toString
    case JDouble(x) if (targetType == classOf[Number]) => x
    case JString(s) if (targetType == classOf[String]) => s
    case JString(s) if (targetType == classOf[Symbol]) => Symbol(s)
    case JString(s) if (targetType == classOf[Date]) => formats.dateFormat.parse(s).getOrElse(fail("Invalid date '" + s + "'"))
    case JBool(x) if (targetType == classOf[Boolean]) => x
    case JBool(x) if (targetType == classOf[JavaBoolean]) => new JavaBoolean(x)
    case JNull => null
    case JNothing => fail("Did not find value which can be converted into " + targetType.getName)
    case JField(_, x) => convert(x, targetType, formats)
    case _ => fail("Do not know how to convert " + json + " into " + targetType)
  }
}

}
}
