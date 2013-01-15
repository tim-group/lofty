name := "Lofty"

version := "0.1"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  // NOTE (2013-01-15, msiegel): Expicitly versioned until 2.10.0 versions resolve
  //"org.scalaz" %% "scalaz-core" % "6.0.4",
  //"org.specs2" %% "specs2" % "1.13" % "test"
  "org.scalaz" % "scalaz-core_2.10" % "6.0.4",
  "org.specs2" % "specs2_2.10" % "1.13" % "test"
)

libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)

resolvers ++= Seq("snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
                  "releases"  at "http://oss.sonatype.org/content/repositories/releases")
