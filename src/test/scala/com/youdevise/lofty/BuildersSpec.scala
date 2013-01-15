package com.youdevise.lofty

import org.specs2.mutable._

import com.youdevise.lofty.Builders._

class BuildersSpec extends Specification {

  case class Address(lines:Seq[String], postcode:String)
  case class Person(name:String, age: Option[Int], address:Address)

  implicit object AddressBuilder extends Builder[Address] {
    def buildFrom(properties: Map[String, _]) = Address(properties("lines").asInstanceOf[Seq[String]],
                                                        properties("postcode").asInstanceOf[String])
  }

  implicit object PersonBuilder extends Builder[Person] {
    def buildFrom(properties: Map[String, _]) = Person(properties("name").asInstanceOf[String],
                                                       properties.get("age").asInstanceOf[Option[Int]],
                                                       properties("address").asInstanceOf[Address])
  }

  """A builder""" should {
    """Build an object using all method calls captured within the recorder monad""" in {
      val withoutAge = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")

        person  <- builder[Person]
        _       <- person name "Stephen Patrick"
        _       <- person address address
      } yield person

      buildFrom(withoutAge) must beEqualTo(Person("Stephen Patrick", None, Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX")))

      val withAge = for {
        person <- withoutAge
        _      <- person age 63
      } yield person

      buildFrom(withAge) must beEqualTo(Person("Stephen Patrick", Some(63), Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX")))

      val movedToDulwich = for {
        newAddress <- builder[Address]
        _          <- newAddress lines Seq("42 Penguin Ave", "Dulwich")
        _          <- newAddress postcode("E17 4TW")

        person <- withoutAge
        _      <- person address newAddress
      } yield person

      buildFrom(movedToDulwich) must beEqualTo(Person("Stephen Patrick", None, Address(Seq("42 Penguin Ave", "Dulwich"), "E17 4TW")))
    }
  }
}
