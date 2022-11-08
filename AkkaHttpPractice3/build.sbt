ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "AkkaHttpPractice3"
  )
val akkaVersion = "2.7.0"
val AkkaHttpVersion = "10.4.0"

libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.30"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % "Test",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "Test",
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
)
