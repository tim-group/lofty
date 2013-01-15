package com.youdevise.lofty

import org.specs2.mutable._

import com.youdevise.lofty.Builders._

class BuildersSpec extends Specification {

  case class Hobby(name:String, hoursPerWeek:Int)
  case class Address(lines:Seq[String], postcode:String)
  case class Person(name:String, age: Option[Int], address:Address, hobbies: Set[Hobby] = Set.empty[Hobby])

  implicit object HobbyBuilder extends Builder[Hobby] {
    def buildFrom(properties: Map[String, _]) = Hobby(properties("name").asInstanceOf[String],
                                                      properties("hoursPerWeek").asInstanceOf[Int])
  }

  implicit object AddressBuilder extends Builder[Address] {
    def buildFrom(properties: Map[String, _]) = Address(properties("lines").asInstanceOf[Seq[String]],
                                                        properties("postcode").asInstanceOf[String])
  }

  implicit object PersonBuilder extends Builder[Person] {
    def buildFrom(properties: Map[String, _]) = Person(properties("name").asInstanceOf[String],
                                                       properties.get("age").asInstanceOf[Option[Int]],
                                                       properties("address").asInstanceOf[Address],
                                                       properties.getOrElse("hobbies", Set.empty[Hobby]).asInstanceOf[Set[Hobby]])
  }

  """A builder""" should {
    """Build an object using all method calls captured within the recorder monad""" in {

      val maudlinStreet = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")
      } yield address

      val withoutAge = for {
        address <- maudlinStreet

        person  <- builder[Person]
        _       <- person name "Stephen Patrick"
        _       <- person address address
      } yield person

      buildFrom(withoutAge) must beEqualTo(Person("Stephen Patrick", None, buildFrom(maudlinStreet)))

      val withAge = for {
        person <- withoutAge
        _      <- person age 63
      } yield person

      buildFrom(withAge) must beEqualTo(Person("Stephen Patrick", Some(63), buildFrom(maudlinStreet)))

      val movedToDulwich = for {
        newAddress <- builder[Address]
        _          <- newAddress lines Seq("42 Penguin Ave", "Dulwich")
        _          <- newAddress postcode("E17 4TW")

        person <- withoutAge
        _      <- person address newAddress
      } yield person

      buildFrom(movedToDulwich) must beEqualTo(Person("Stephen Patrick", None, Address(Seq("42 Penguin Ave", "Dulwich"), "E17 4TW")))

      val withHobbies = for {
        hobby1 <- builder[Hobby]
        _      <- hobby1 name "Programming"
        _      <- hobby1 hoursPerWeek 5


        hobby2 <- builder[Hobby]
        _      <- hobby2 name "Ice skating"
        _      <- hobby2 hoursPerWeek 2

        person <- withoutAge
        _      <- person hobbies Set(hobby1, hobby2)
      } yield person

      buildFrom(withHobbies) must beEqualTo(Person("Stephen Patrick",
                                                   None,
                                                   buildFrom(maudlinStreet),
                                                   Set(Hobby("Programming", 5), Hobby("Ice skating", 2))))
    }
  }
}
