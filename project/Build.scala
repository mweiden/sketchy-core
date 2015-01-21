import sbt._
import sbtassembly.Plugin._
import AssemblyKeys._
import Keys._
import sbtrelease.ReleasePlugin._

object BuildSettings {

  val buildSettings = Defaults.defaultSettings

  lazy val assemblySettings = sbtassembly.Plugin.assemblySettings ++ Seq(
    excludedFiles in assembly := { (bases: Seq[File]) =>
      bases flatMap { base => (
        (base ** "log4j.properties") +++
        (base / "META-INF")).get
      }
    },
    mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
        case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
        case x if x.endsWith(".html") => MergeStrategy.first
        case x => old(x)
      }
    },
    publishTo <<= version { v =>
      val repo = envGetOrElse("MAVEN_URL", "<MAVEN_URL>/")
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at repo + "snapshots")
      else
        Some("releases" at repo + "releases")
    },
    autoScalaLibrary := false
  )

  def envGetOrElse(key: String, default: String) =
    if (System.getenv(key) != null) System.getenv(key) else default
}

object SketchyBuild extends Build {
  import BuildSettings._

  lazy val root = Project(
    "sketchy",
    file(".")).aggregate(sketchyCore, sketchyExample)

  lazy val sketchyExample = Project(
    "sketchy-example",
    file("example"),
    settings = buildSettings ++ assemblySettings).dependsOn(sketchyCore)
      .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)

  lazy val sketchyCore = Project(
    "sketchy-core",
    file("core"),
    settings = buildSettings ++ releaseSettings ++ assemblySettings)
      .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
}
