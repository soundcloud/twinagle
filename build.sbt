lazy val scala212 = "2.12.15"
lazy val scala213 = "2.13.8"

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
    addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4"),
    libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion,
    buildInfoKeys                                 := Seq[BuildInfoKey](version, scalaBinaryVersion),
    buildInfoPackage                              := "com.soundcloud.twinagle.codegen",
    buildInfoUsePackageAsPath                     := true,
    publishLocal                                  := publishLocal.dependsOn(runtime / publishLocal).value,
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false
  )

lazy val runtime = (project in file("runtime")).settings(
  commonSettings,
  name               := "twinagle-runtime",
  crossScalaVersions := Seq(scala212, scala213),
  libraryDependencies ++= Seq(
    "com.twitter"          %% "finagle-http"    % "21.11.0",
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-json4s"  % "0.12.0",
    "org.json4s"           %% "json4s-native"   % "4.0.3",
    "org.specs2"           %% "specs2-core"     % "4.13.1" % Test,
    "org.specs2"           %% "specs2-mock"     % "4.13.1" % Test
  ),
  // compile protobuf messages for unit tests
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  Test / PB.targets := Seq(
    scalapb.gen(flatPackage = true) -> (Test / sourceManaged).value
  )
)

lazy val root = (project in file("."))
  .aggregate(runtime, codegen)
  .settings(
    name := "twinagle-root",
    resolvers += Resolver.typesafeIvyRepo("releases"),
    publish / skip := true
  )
