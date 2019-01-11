ThisBuild / organization := "com.soundcloud.twinagle"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / crossScalaVersions := Seq("2.11.11")

val scalapbVersion = "0.8.3"

lazy val codegen = (project in file("codegen"))
  .enablePlugins(ScriptedPlugin)
  .settings(
    name := "twinagle-scalapb-plugin",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % scalapbVersion
    ),
    scriptedSbt := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.2.7"
      }
    },
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )

lazy val runtime = (project in file("runtime")).settings(
  name := "twinagle-runtime",
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-http" % "18.12.0",
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.2",

    "org.specs2" %% "specs2-core" % "4.3.6" % Test
  )
)


lazy val root = (project in file("."))
  .aggregate(runtime, codegen)
  .settings(
    name := "twinagle-root",
    resolvers += Resolver.typesafeIvyRepo("releases"),
  )
