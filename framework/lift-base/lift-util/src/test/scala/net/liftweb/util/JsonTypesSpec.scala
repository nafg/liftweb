/*
 * Copyright 2007-2010 WorldWide Conferencing, LLC
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

import _root_.org.specs._
import _root_.org.specs.runner._
import common._
import json.JsonParser.parse
import json.Serialization.{write => swrite}

class JsonTypesSpecTest extends JUnit4(JsonTypesSpec)
object JsonTypesSpec extends Specification {
  implicit val formats = net.liftweb.json.DefaultFormats + new JsonBoxSerializer

  "Extract empty age" in {
    parse("""{"name":"joe"}""").extract[Person] mustEqual Person("joe", Empty)
  }

  "Render with age" in {
    swrite(Person("joe", Full(12))) mustEqual """{"name":"joe","age":12}"""
  }
}

case class Person(name: String, age: Box[Int])

}
}
