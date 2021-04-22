name := "TrueAccordAssignment"
version := "0.1"
scalaVersion := "2.13.5"
// idePackagePrefix := Some("com.trueaccord")

val verJson4s = "3.+"
lazy val libJson4s = Seq(
  "org.json4s" %% "json4s-core" % verJson4s,
  "org.json4s" %% "json4s-native" % verJson4s,
  "org.json4s" %% "json4s-jackson" % verJson4s
)
// resolvers in ThisBuild ++= Seq( Resolver.maveb)
val verAkka = "10.1.+"
lazy val libAkka = Seq(
  "com.typesafe.akka" %% "akka-http" % verAkka,
  "com.typesafe.akka" %% "akka-http-testkit" % verAkka,
  "com.typesafe.akka" %% "akka-stream" % "2.6.+"
)

val verScalaTest = "3.+"
lazy val libScalaTest = Seq(
  "org.scalatest" %% "scalatest" % verScalaTest % Test,
  "org.scalatest" %% "scalatest-funsuite" % verScalaTest % Test
)

val verScalaMock = "5.+"
lazy val libScalaMock = Seq(
  "org.scalamock" %% "scalamock" % verScalaMock % Test
)

libraryDependencies in ThisBuild ++= (
  libAkka ++
  libJson4s ++
  libScalaTest ++
  libScalaMock
)

fork in (Compile, run) := true