ThisBuild / organization := "com.soundcloud.twinagle"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / crossScalaVersions := Seq("2.11.11")

val scalapbVersion = "0.8.3"

lazy val codegen = (project in file("codegen"))
  .enablePlugins(ScriptedPlugin, BuildInfoPlugin)
  .settings(
    name := "twinagle-scalapb-plugin",

    sbtPlugin := true,
    crossSbtVersions := List(sbtVersion.value, "0.13.18"),
    addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.19"),
    libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion,

    buildInfoKeys := Seq[BuildInfoKey](version, scalaBinaryVersion),
    buildInfoPackage := "com.soundcloud.twinagle.codegen",
    buildInfoUsePackageAsPath := true,

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
  publishTo in ThisBuild := Some("SC snapshots" at "https://maven.dev.s-cloud.net/content/repositories/snapshots"),
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-http" % "18.12.0",
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.2",

    "org.specs2" %% "specs2-core" % "4.3.6" % Test,
    "org.specs2" %% "specs2-mock" % "4.3.6" % Test
  )
)


lazy val root = (project in file("."))
  .aggregate(runtime, codegen)
  .settings(
    name := "twinagle-root",
    resolvers += Resolver.typesafeIvyRepo("releases"),
    skip in publish := true
  )
