import sbtassembly.Plugin._
import AssemblyKeys._

version := "0.4.7-SNAPSHOT"

organization := "com.soundcloud"

scalaVersion := "2.10.4"

exportJars := true

jarName in assembly := "sketchy-core.jar"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize")

parallelExecution in Test := false

releaseSettings

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Big Bee Consultants" at "http://www.bigbeeconsultants.co.uk/repo",
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
  "com.typesafe.play" % "play-json_2.10" % "2.3.0",
  "org.scalaj" % "scalaj-time_2.10.0-M7" % "0.7-SNAPSHOT",
  "org.scalatest" %% "scalatest" % "2.0.M6" % "test")

// Actors
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % "2.10.4")

// Memcached
libraryDependencies ++= Seq(
  "com.thimbleware.jmemcached" % "jmemcached-core" % "1.0.0",
  "net.spy" % "spymemcached" % "2.10.0")

// metrics
libraryDependencies ++= Seq(
  "io.prometheus" % "client" % "0.0.4")

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
  "com.novocode" % "junit-interface" % "0.10-M1" % "test",
  "com.h2database" % "h2" % "1.4.184")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a")

// HTTP
libraryDependencies ++= Seq(
  "uk.co.bigbeeconsultants" %% "bee-client" % "0.26.1")

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.3.0",
  "org.scalatra" %% "scalatra-scalatest" % "2.3.0" % "test",
  "org.scalatra" %% "scalatra-specs2" % "2.3.0" % "test")
