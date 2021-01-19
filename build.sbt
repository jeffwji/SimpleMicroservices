// DEPENDENCIES
val ver = new{
  val logback = "1.2.3"
  val scalaLogging = "3.9.2"
  val scalaTest = "3.2.3"
  val zio = "1.0.3"
  val zioInteropCats = "2.2.0.1"
  val doobie = "0.9.4"
  val config = "1.4.1"
  val circe = "0.13.0"
  val akka = "2.6.10"
  val akkaHttp = "10.2.1"
  val mysql = "8.0.23"
}

lazy val testing = Seq(
  "org.scalatest" %% "scalatest" % ver.scalaTest,
  "com.typesafe.akka" %% "akka-http-testkit" % ver.akkaHttp,
  "dev.zio" %% "zio-test" % ver.zio,
  "dev.zio" %% "zio-test-sbt" % ver.zio,
  "org.tpolecat" %% "doobie-h2" % ver.doobie
)

lazy val logging = Seq(
  "ch.qos.logback" % "logback-classic" % ver.logback,
  "com.typesafe.scala-logging" %% "scala-logging" % ver.scalaLogging
)

lazy val config = Seq(
  "com.typesafe" % "config" % ver.config
)

lazy val zio = Seq(
  "dev.zio" %% "zio" % ver.zio,
  "dev.zio" %% "zio-streams" % ver.zio,
  "dev.zio" %% "zio-interop-cats" % ver.zioInteropCats
)

lazy val jdbc = Seq(
  "org.tpolecat" %% "doobie-core" % ver.doobie,
  "org.tpolecat" %% "doobie-postgres" % ver.doobie,
  "org.tpolecat" %% "doobie-hikari" % ver.doobie
)

lazy val mysql = Seq(
  "mysql" % "mysql-connector-java" % ver.mysql
)

lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor" % ver.akka,
  "com.typesafe.akka" %% "akka-stream" % ver.akka,
  "com.typesafe.akka" %% "akka-http" % ver.akkaHttp,
  "de.heikoseeberger" %% "akka-http-circe" % "1.35.2"
)

lazy val circe = Seq(
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % ver.circe,
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % ver.circe
)

lazy val SimpleMicroservices = project.in(file(".")).settings(
  name := "SimpleMicroservices",
  version := "1.0.0",
  scalaVersion := "2.13.4",
  libraryDependencies ++= testing.map(_ % Test) ++ logging ++ config ++ zio ++ jdbc ++ mysql ++ akka ++ circe,
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)
