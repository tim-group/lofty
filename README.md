lofty
=====

[![Build Status](https://travis-ci.org/youdevise/lofty.png)](https://travis-ci.org/youdevise/lofty)

A dynamic DSL for building complex objects in Scala 2.10. For a discussion of the aims and methods of the library, see the post [Making it easy with Lofty](https://devblog.timgroup.com/2013/01/16/making-it-easy-with-lofty/) on the [TIM Group blog](https://devblog.timgroup.com).

Example usage:

```scala
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
```
