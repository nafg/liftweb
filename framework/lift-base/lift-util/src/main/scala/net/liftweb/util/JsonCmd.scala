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

import common._
import scala.xml.{NodeSeq, Text}
import java.util.regex.Pattern

trait HasParams {
  def param(name: String): Box[String]
}


/**
 * Impersonates a JSON command
 */
case class JsonCmd(command: String, target: String, params: Any,
                   all: _root_.scala.collection.Map[String, Any])

/**
 * Holds information about a response
 */
class ResponseInfoHolder {
  var headers: Map[String, String] = Map.empty
  private var _docType: Box[String] = Empty
  private var _setDocType = false

  def docType = _docType

  def docType_=(in: Box[String]) {
    _docType = in
    _setDocType = true
  }

  def overrodeDocType = _setDocType
}

trait HasMaxLen {
  def maxLen: Int
}

trait StringFieldHelpers {
  self: FieldIdentifier with HasMaxLen =>

  final def crop(in: String): String = in.substring(0, Math.min(in.length, maxLen))

  final def removeRegExChars(regEx: String)(in: String): String = in.replaceAll(regEx, "")

  final def toLower(in: String): String = in match {
    case null => null
    case s => s.toLowerCase
  }
  final def toUpper(in: String): String = in match {
    case null => null
    case s => s.toUpperCase
  }

  final def trim(in: String): String = in match {
    case null => null
    case s => s.trim
  }

  final def notNull(in: String): String = in match {
    case null => ""
    case s => s
  }


  /**
   * A validation helper.  Make sure the string is at least a particular
   * length and generate a validation issue if not
   */
  def valMinLen(len: Int, msg: => String)(value: String): List[FieldError] =
  if ((value eq null) || value.length < len) List(FieldError(this, Text(msg)))
  else Nil

  /**
   * A validation helper.  Make sure the string is no more than a particular
   * length and generate a validation issue if not
   */
  def valMaxLen(len: Int, msg: => String)(value: String): List[FieldError] =
  if ((value ne null) && value.length > len) List(FieldError(this, Text(msg)))
  else Nil

  /**
   * Make sure the field matches a regular expression
   */
  def valRegex(pat: Pattern, msg: => String)(value: String): List[FieldError] = pat.matcher(value).matches match {
    case true => Nil
    case false => List(FieldError(this, Text(msg)))
  }

  final def cropb(in: Box[String]): Box[String] = in.map(in => in.substring(0, Math.min(in.length, maxLen)))

  final def removeRegExCharsb(regEx: String)(in: Box[String]): Box[String] = in.map(_.replaceAll(regEx, ""))

  final def toLowerb(in: Box[String]): Box[String] = in.map {
    case null => null
    case s => s.toLowerCase
  }

  final def toUpperb(in: Box[String]): Box[String] = in map {
    case null => null
    case s => s.toUpperCase
  }

  final def trimb(in: Box[String]): Box[String] = in map {
    case null => null
    case s => s.trim
  }

  final def notNullb(in: Box[String]): Box[String] = in match {
    case Full(x) if null eq x => Full("")
    case Full(x) => Full(x)
    case _ => Full("")
  }


  /**
   * A validation helper.  Make sure the string is at least a particular
   * length and generate a validation issue if not
   */
  def valbMinLen(len: Int, msg: => String)(value: Box[String]): List[FieldError] =
  value match {
    case Full(value) => if ((value eq null) || value.length < len) List(FieldError(this, Text(msg))) else Nil
    case _ => List(FieldError(this, Text(msg)))
  }

  /**
   * A validation helper.  Make sure the string is no more than a particular
   * length and generate a validation issue if not
   */
  def valbMaxLen(len: Int, msg: => String)(value: Box[String]): List[FieldError] =
  for {
    v <- value.toList
    ret <-  if ((v ne null) && v.length > len) List(FieldError(this, Text(msg))) else Nil
  } yield ret


  /**
   * Make sure the field matches a regular expression
   */
  def valbRegex(pat: Pattern, msg: => String)(value: Box[String]): List[FieldError] = value match {
    case Full(value) =>
      pat.matcher(value).matches match {
        case true => Nil
        case false => List(FieldError(this, Text(msg)))
      }
    case _ => List(FieldError(this, Text(msg)))
  }
}

/**
 * Defines the association of this reference with an markup tag ID
 */
trait FieldIdentifier {
  def uniqueFieldId: Box[String] = Empty
}

/**
 * Associate a FieldIdentifier with an NodeSeq
 */
case class FieldError(field: FieldIdentifier, msg: NodeSeq) {
  override def toString = field.uniqueFieldId + " : " + msg

  override def hashCode(): Int = msg.hashCode + field.uniqueFieldId.hashCode
  override def equals(other: Any): Boolean = other match {
    case FieldError(of, om) => (om == msg) && (of.uniqueFieldId == field.uniqueFieldId)
    case _ => super.equals(other)
  }
}

object FieldError {
  import scala.xml.Text
  def apply(field: FieldIdentifier, msg: String) = new FieldError(field, Text(msg))
}

trait FieldContainer {
  def allFields: Seq[BaseField]
}

trait BaseField extends FieldIdentifier with SettableValueHolder with FieldContainer {
  import scala.xml.Text

  /**
   * Will be set to the type of the field
   */
  type ValueType


  /**
   * A list of functions that transform the value before it is set.  The transformations
   * are also applied before the value is used in a query.  Typical applications
   * of this are trimming and/or toLowerCase-ing strings
   */
  protected def setFilter: List[ValueType => ValueType]


  def validations: List[ValueType => List[FieldError]]


  /**
   * Is the Field required (and will have a style designating it as such)
   */
  def required_? = false

  /**
   * Is this an upload field so that a form that includes this field must be multi-part mime
   */
  def uploadField_? = false


  /**
   * Validate this field and return a list of Validation Issues
   */
  def validate: List[FieldError]

  /**
   * The human name of this field
   */
  def name: String

  def helpAsHtml: Box[NodeSeq] = Empty


  /**
   * Create an input field for the item
   */
  def toForm: Box[NodeSeq]

  /**
   * A unique 'id' for the field for form generation
   */
  def fieldId: Option[NodeSeq] = None

  def displayNameHtml: Box[NodeSeq] = Empty

  def displayHtml: NodeSeq = displayNameHtml openOr Text(displayName)


  /**
   * The display name of this field (e.g., "First Name")
   */
  def displayName: String = name

  def allFields: Seq[BaseField] = List(this)
}

}
}
