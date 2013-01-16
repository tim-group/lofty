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
    """Build an object using the method calls captured within the recorder monad""" in {

      val personBuilder = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")

        person  <- builder[Person]
        _       <- person name "Stephen Patrick"
        _       <- person address address
      } yield person

      buildFrom(personBuilder) must beEqualTo(Person(
        "Stephen Patrick",
        None,
        Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX")))
    }

    """Be able to compose recordings within the recorder monad""" in {
      val addressBuilder = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")
      } yield address

      val personBuilder = for {
      address <- addressBuilder
      person  <- builder[Person]
      _       <- person name "Stephen Patrick"
      _       <- person address address
      } yield person

      buildFrom(personBuilder) must beEqualTo(Person(
        "Stephen Patrick",
        None,
        Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX")))
    }

    """Be able to extend an existing recording, filling in missing values""" in {
      val withoutAge = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")

        person  <- builder[Person]
        _       <- person name "Stephen Patrick"
        _       <- person address address
      } yield person

      val withAge = for {
        person <- withoutAge
        _      <- person age 63
      } yield person

      buildFrom(withAge) must beEqualTo(Person(
        "Stephen Patrick",
        Some(63),
        Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX")))
    }

    """Be able to override details of an existing recording""" in {
      val personBuilder = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")

        person  <- builder[Person]
        _       <- person name "Stephen Patrick"
        _       <- person address address
      } yield person

      val movedToDulwich = for {
        newAddress <- builder[Address]
        _          <- newAddress lines Seq("42 Penguin Ave", "Dulwich")
        _          <- newAddress postcode("E17 4TW")

        person <- personBuilder
        _      <- person address newAddress
      } yield person

      buildFrom(movedToDulwich) must beEqualTo(Person(
        "Stephen Patrick",
        None,
        Address(Seq("42 Penguin Ave", "Dulwich"), "E17 4TW")))
    }

    """Convert iterables of builders into reified objects when building""" in {
      val personBuilder = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")

        person  <- builder[Person]
        _       <- person name "Stephen Patrick"
        _       <- person address address
      } yield person

      val withHobbies = for {
        hobby1 <- builder[Hobby]
        _      <- hobby1 name "Programming"
        _      <- hobby1 hoursPerWeek 5


        hobby2 <- builder[Hobby]
        _      <- hobby2 name "Ice skating"
        _      <- hobby2 hoursPerWeek 2

        person <- personBuilder
        _      <- person hobbies Set(hobby1, hobby2)
      } yield person

      buildFrom(withHobbies) must beEqualTo(Person(
        "Stephen Patrick",
        None,
        Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX"),
        Set(Hobby("Programming", 5), Hobby("Ice skating", 2))))
    }

    """Convert a 2Tuple of builders into a 2Tuple of objects""" in {
      val personAndAddressBuilder = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")

        person  <- builder[Person]
        _       <- person name "Stephen Patrick"
        _       <- person address address
      } yield (person, address)

      val (person, address) = buildFrom(personAndAddressBuilder)

      address must beEqualTo(Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX"))
      person must beEqualTo(Person(
        "Stephen Patrick",
        None,
        address))
    }

    """Convert a 3Tuple of builders into a 3Tuple of objects""" in {
      val tupleBuilder = for {
        address <- builder[Address]
        _       <- address lines Seq("12 Maudlin Street", "Manchester")
        _       <- address postcode("VB6 5UX")

        hobby1 <- builder[Hobby]
        _      <- hobby1 name "Programming"
        _      <- hobby1 hoursPerWeek 5

        person  <- builder[Person]
        _       <- person name "Stephen Patrick"
        _       <- person address address
        _       <- person hobbies Set(hobby1)
      } yield (person, address, hobby1)

      val (person, address, hobby) = buildFrom(tupleBuilder)

      address must beEqualTo(Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX"))
      hobby must beEqualTo(Hobby("Programming", 5))
      person must beEqualTo(Person(
        "Stephen Patrick",
        None,
        address,
      Set(hobby)))
    }

    """Read properties as by-reference values""" in {
      val personBuilder = for {
        me     <- builder[Person]
        _      <- me name "My name"
        _      <- me address (Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX"))

        you       <- builder[Person]
        _         <- you name "Your name"
        _         <- you address me.address
      } yield (me, you)

      val (me, you) = buildFrom(personBuilder)

      me.address must beEqualTo(Address(Seq("12 Maudlin Street", "Manchester"), "VB6 5UX"))
      you.address must beEqualTo(me.address)

      val afterMoving = for {
        (me, you) <- personBuilder
        _         <- me address Address(Seq("42 Penguin Ave", "Dulwich"), "E17 4TW")
      } yield (me, you)

      val (movedMe, movedYou) = buildFrom(afterMoving)

      movedMe.address must beEqualTo(Address(Seq("42 Penguin Ave", "Dulwich"), "E17 4TW"))
      movedYou.address must beEqualTo(movedMe.address)
    }
  }
}
