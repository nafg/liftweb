/*
 * Copyright 2006-2010 WorldWide Conferencing, LLC
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
package util {

import scala.reflect.Manifest
import common._
import json.Extraction.{decompose, extract}
import json.{Formats, TypeInfo, Serializer}
import json.JsonAST._

class JsonBoxSerializer extends Serializer {
  private val BoxClass = classOf[Box[_]]
  import scala.collection.jcl.Conversions._

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Any] = {
    case (t @ TypeInfo(BoxClass, Some(ptype)), json) => json match {
      case JNull | JNothing => Empty
      case JObject(JField("$box_failure", JString("Failure")) :: 
                   JField("msg", JString(msg)) :: 
                   JField("exception", JArray(exceptions)) ::
                   JField("chain", JArray(chain)) :: Nil) => Failure(msg)
      case JObject(JField("$box_failure", JString("ParamFailure")) :: 
                   JField("msg", JString(msg)) :: 
                   JField("exception", JArray(exceptions)) ::
                   JField("chain", JArray(chain)) :: 
                   JField("param", param) :: Nil) => ParamFailure(msg, null)
      case x => Full(extract(x, TypeInfo(ptype.getActualTypeArguments()(0).asInstanceOf[Class[_]], None)))
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case Full(x) => decompose(x)
    case Empty => JNothing
  }
} 

}
}
