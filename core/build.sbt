import sbtassembly.Plugin._
import AssemblyKeys._

version := "0.6.3"

organization := "com.soundcloud"

scalaVersion := "2.11.7"

exportJars := true

jarName in assembly := "sketchy-core.jar"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize")

parallelExecution in Test := false

releaseSettings

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Big Bee Consultants" at "http://dl.bintray.com/rick-beton/maven/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Maven Central Server" at "http://repo1.maven.org/maven2",
  "mweiden mvn-repo" at "https://raw.github.com/mweiden/mvn-repo/master/releases")

// base
libraryDependencies ++= Seq(
  "javax.mail" % "mail" % "1.4",
  "commons-lang" % "commons-lang" % "2.5",
  "commons-codec" % "commons-codec" % "1.9",
  "joda-time" % "joda-time" % "2.1",
  "junit" % "junit" % "4.10" % "test",
  "com.typesafe.play" %% "play-json" % "2.3.0",
  "org.scalaj" %% "scalaj-time" % "0.5",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test")

// Actors
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % scalaVersion.value)

// Memcached
libraryDependencies ++= Seq(
  "com.thimbleware.jmemcached" % "jmemcached-core" % "1.0.0",
  "net.spy" % "spymemcached" % "2.10.0")

// metrics
libraryDependencies ++= Seq(
  "io.prometheus" % "simpleclient" % "0.0.13")

// RabbitMQ
libraryDependencies ++= Seq(
  "com.rabbitmq" % "amqp-client" % "3.3.5",
  "net.jodah" % "lyra" % "0.4.2")

// database
libraryDependencies ++= Seq(
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "commons-pool" % "commons-pool" % "1.6",
  "mysql" % "mysql-connector-java" % "5.1.20")

libraryDependencies ++= List(
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe.slick" %% "slick-testkit" % "2.1.0" % "test",
  "com.h2database" % "h2" % "1.4.184")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a")

// HTTP
libraryDependencies ++= Seq(
  "uk.co.bigbeeconsultants" %% "bee-client" % "0.29.1")

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.3.0",
  "org.scalatra" %% "scalatra-scalatest" % "2.3.0" % "test",
  "org.scalatra" %% "scalatra-specs2" % "2.3.0" % "test")


libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "slf4j-log4j12" % "1.7.10")
