import sbtassembly.Plugin._
import AssemblyKeys._

version := "0.4.2"

organization := "com.soundcloud"

scalaVersion := "2.10.1"

exportJars := true

jarName in assembly := "sketchy-example.jar"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize")

resolvers ++= Seq(
  "Sonatype Snapshots"   at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe Repository"  at "http://repo.typesafe.com/typesafe/snapshots/",
  "Maven Central Server" at "http://repo1.maven.org/maven2",
  "mweiden mvn-repo" at "https://raw.github.com/mweiden/mvn-repo/master/releases")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.6.6",
  "net.liftweb" %% "lift-json" % "3.0-SNAPSHOT",
  "net.spy" % "spymemcached" % "2.8.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.2" % "runtime",
  "junit" % "junit" % "4.7" % "test",
  "org.scalatest" %% "scalatest" % "2.0.M6" % "test",
  "commons-lang" % "commons-lang" % "2.5")

libraryDependencies ++= Seq(
  "org.scalanlp" % "nak" % "1.1.3")

libraryDependencies ++= Seq(
  "io.prometheus" % "client" % "0.0.2",
  "io.prometheus.client.utility" % "servlet" % "0.0.2")

libraryDependencies ++= Seq(
  "net.joshdevins.rabbitmq" % "rabbitmq-ha-client" % "0.1.0",
  "log4j" % "log4j" % "1.2.15",
  "com.rabbitmq" % "amqp-client" % "2.8.4")

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.20",
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "commons-pool" % "commons-pool" % "1.6",
  "com.h2database" % "h2" % "1.3.168")

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.2.1",
  "org.scalatra" %% "scalatra-scalatest" % "2.2.1" % "test",
  "org.scalatra" %% "scalatra-specs2" % "2.2.1" % "test")

libraryDependencies ++= Seq(
  "org.mockito" % "mockito-all" % "1.9.0")

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-webapp" % "7.6.0.v20120127",
  "javax.servlet" % "servlet-api" % "2.5" % "provided")

