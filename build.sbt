lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"

lazy val commonSettings = List(
  scalaVersion := scala212,
  scalacOptions ++= Seq(
    "-encoding", "utf8",
    "-deprecation",
    "-unchecked",
    "-Xlint",
    "-Xfatal-warnings"
  ),
  Compile / console / scalacOptions --= Seq("-deprecation", "-Xfatal-warnings", "-Xlint")
)

lazy val codegen = (project in file("codegen"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    commonSettings,
    name := "twinagle-scalapb-plugin",

    addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.27"),
    libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion,

    buildInfoKeys := Seq[BuildInfoKey](version, scalaBinaryVersion),
    buildInfoPackage := "com.soundcloud.twinagle.codegen",
    buildInfoUsePackageAsPath := true,

    publishLocal := publishLocal.dependsOn(publishLocal in runtime).value,
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false
  )

lazy val runtime = (project in file("runtime")).settings(
  commonSettings,
  name := "twinagle-runtime",
  crossScalaVersions := Seq(scala212, scala213),
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-http" % "20.1.0",
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.10.0",

    "org.specs2" %% "specs2-core" % "4.9.2" % Test,
    "org.specs2" %% "specs2-mock" % "4.9.2" % Test
  )
)


lazy val root = (project in file("."))
  .aggregate(runtime, codegen)
  .settings(
    name := "twinagle-root",
    resolvers += Resolver.typesafeIvyRepo("releases"),
    skip in publish := true
  )

// publishing to maven central

ThisBuild / organization := "com.soundcloud"
ThisBuild / organizationName := "SoundCloud"
ThisBuild / organizationHomepage := Some(url("https://developers.soundcloud.com/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/soundcloud/twinagle"),
    "scm:git@github.com:soundcloud/twinagle.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "ccmtaylor",
    name  = "Christopher Taylor",
    email = "christopher.taylor@soundcloud.com",
    url   = url("https://github.com/ccmtaylor")
  ),
  Developer(
    id    = "oberkowitz",
    name  = "Oren Berkowitz",
    email = "oren.berkowitz@soundcloud.com",
    url   = url("https://github.com/oberkowitz")
  )
)

usePgpKeyHex("612C04F1EFE66FB7")
ThisBuild / description := "An implementation of the Twirp protocol on top of Finagle"
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/soundcloud/twinagle"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / publishMavenStyle := true


import ReleaseTransformations._

releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  releaseStepInputTask(codegen/scripted),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // For non cross-build projects, use releaseStepCommand("publishSigned")
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
