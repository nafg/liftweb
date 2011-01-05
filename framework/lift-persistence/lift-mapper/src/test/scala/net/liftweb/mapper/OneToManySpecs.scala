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
package mapper {

import org.specs._
import _root_.org.specs.runner.{JUnit3, ConsoleRunner}

class OneToManySpecsAsTest extends JUnit3(OneToManySpecs)
object OneToManySpecsRunner extends ConsoleRunner(OneToManySpecs)

object OneToManySpecs extends Specification {

  val provider = DBProviders.H2MemoryProvider
  
  private def ignoreLogger(f: => AnyRef): Unit = ()
  def setupDB {
    MapperRules.createForeignKeys_? = c => false
    provider.setupDB
    Schemifier.destroyTables_!!(ignoreLogger _,  Contact, Phone)
    Schemifier.schemify(true, ignoreLogger _, Contact, Phone)
  }

  "OneToMany" should {
    "detect all MappedOneToMany fields" in {
      setupDB
      val contact = Contact.create
      val fields = contact.oneToManyFields
      fields.length must_== 1
      fields(0).asInstanceOf[Any] must_== contact.phones
    }
  }

}



class Contact extends LongKeyedMapper[Contact] with IdPK with OneToMany[Long, Contact] {
  def getSingleton = Contact
  object phones extends MappedOneToMany(Phone, Phone.contact)
}
object Contact extends Contact with LongKeyedMetaMapper[Contact]

class Phone extends LongKeyedMapper[Phone] with IdPK {
  def getSingleton = Phone
  object contact extends LongMappedMapper(this, Contact)
  object number extends MappedString(this, 10)
}
object Phone extends Phone with LongKeyedMetaMapper[Phone]


}
}
