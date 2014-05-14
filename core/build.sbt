import sbtassembly.Plugin._
import AssemblyKeys._

version := "0.4.5"

organization := "com.soundcloud"

scalaVersion := "2.10.1"

exportJars := true

jarName in assembly := "sketchy-core.jar"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize")

parallelExecution in Test := false

releaseSettings

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Big Bee Consultants" at "http://www.bigbeeconsultants.co.uk/repo",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Maven Central Server" at "http://repo1.maven.org/maven2",
  "mweiden mvn-repo" at "https://raw.github.com/mweiden/mvn-repo/master/releases")

// base
libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.2" % "runtime",
  "commons-lang" % "commons-lang" % "2.5",
  "joda-time" % "joda-time" % "2.1",
  "junit" % "junit" % "4.10" % "test",
  "net.liftweb" %% "lift-json" % "3.0-SNAPSHOT",
  "net.liftweb" %% "lift-util" % "3.0-SNAPSHOT",
  "org.scalaj" % "scalaj-time_2.10.0-M7" % "0.7-SNAPSHOT",
  "org.slf4j" % "slf4j-simple" % "1.6.6",
  "org.scalatest" %% "scalatest" % "2.0.M6" % "test")

// Actors
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % "2.10.1")

// Memcached
libraryDependencies ++= Seq(
  "com.thimbleware.jmemcached" % "jmemcached-core" % "1.0.0",
  "net.spy" % "spymemcached" % "2.10.0")

// metrics
libraryDependencies ++= Seq(
  "io.prometheus" % "client" % "0.0.4",
  "io.prometheus.client.utility" % "servlet" % "0.0.2")

// RabbitMQ
libraryDependencies ++= Seq(
  "com.rabbitmq" % "amqp-client" % "2.8.4",
  "net.joshdevins.rabbitmq" % "rabbitmq-ha-client" % "0.1.0" excludeAll(
    ExclusionRule(organization = "com.sun.jdmk"),
    ExclusionRule(organization = "com.sun.jmx"),
    ExclusionRule(organization = "javax.jms")))

// database
libraryDependencies ++= Seq(
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "commons-pool" % "commons-pool" % "1.6",
  "mysql" % "mysql-connector-java" % "5.1.20")

libraryDependencies ++= List(
  "com.typesafe.slick" %% "slick" % "2.0.0-M2",
  "com.typesafe.slick" %% "slick-testkit" % "2.0.0-M2" % "test",
  "com.novocode" % "junit-interface" % "0.10-M1" % "test",
  "ch.qos.logback" % "logback-classic" % "0.9.28" % "test",
  "com.h2database" % "h2" % "1.3.168")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a")

// HTTP
libraryDependencies ++= Seq(
  "uk.co.bigbeeconsultants" %% "bee-client" % "0.26.1",
  "com.typesafe" %% "scalalogging-log4j" % "1.1.0")


libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.2.1",
  "org.scalatra" %% "scalatra-scalatest" % "2.2.1" % "test",
  "org.scalatra" %% "scalatra-specs2" % "2.2.1" % "test")

publishTo <<= version { v =>
  val repo = "http://some.repo"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at repo + "snapshots")
  else
    Some("releases" at repo + "releases")
}

