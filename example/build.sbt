import sbtassembly.Plugin._
import AssemblyKeys._

version := "0.4.2"

organization := "com.soundcloud"

scalaVersion := "2.11.6"

exportJars := true

jarName in assembly := "sketchy-example.jar"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize")

resolvers ++= Seq(
  "Sonatype Snapshots"   at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe Repository"  at "http://repo.typesafe.com/typesafe/snapshots/",
  "Maven Central Server" at "http://repo1.maven.org/maven2",
  "mweiden mvn-repo" at "https://raw.github.com/mweiden/mvn-repo/master/releases")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.scalatra" %% "scalatra-scalatest" % "2.3.0" % "test",
  "org.scalanlp" % "nak" % "1.1.3")

libraryDependencies ++= Seq(
  "io.prometheus" % "simpleclient_servlet" % "0.0.8")

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.20",
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "commons-pool" % "commons-pool" % "1.6")

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.7.v20120910")

