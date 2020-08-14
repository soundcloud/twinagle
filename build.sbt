lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"

lazy val commonSettings = List(
  scalaVersion := scala212,
  scalacOptions ++= Seq(
    "-encoding",
    "utf8",
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
    addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.34"),
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
    "com.twitter"          %% "finagle-http"    % "20.4.1",
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.10.1",

    "org.specs2" %% "specs2-core" % "4.10.3" % Test,
    "org.specs2" %% "specs2-mock" % "4.10.3" % Test
  ),
  // compile protobuf messages for unit tests
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  PB.targets in Test := Seq(
    scalapb.gen(flatPackage = true) -> (sourceManaged in Test).value
  )
)

lazy val root = (project in file("."))
  .aggregate(runtime, codegen)
  .settings(
    name := "twinagle-root",
    resolvers += Resolver.typesafeIvyRepo("releases"),
    skip in publish := true
  )
