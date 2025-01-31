import sbt.CrossVersion

lazy val scala212 = "2.12.18"
lazy val scala213 = "2.13.7"
lazy val scala213dot13 = "2.13.14"
lazy val scala3 = "3.3.4"

lazy val commonSettings = List(
  scalaVersion := scala212,
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => scalacOptions ++= Seq("-Xlint")
      case Some((3, _)) => scalacOptions ++= Seq("-rewrite", "-source:3.0-migration")
      case _ => ()
    }
    Seq(
      "-encoding",
      "utf8",
      "-deprecation",
      "-unchecked",
      "-Xfatal-warnings"
    )
  },
  excludeDependencies += "org.scala-lang.modules" % "scala-collection-compat_2.13",
  Compile / console / scalacOptions --= Seq("-deprecation", "-Xfatal-warnings", "-Xlint"),
  scalafmtOnCompile := true
)

lazy val codegen = (project in file("codegen"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    commonSettings,
    name := "twinagle-scalapb-plugin",
    addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7"),
    libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion,
    buildInfoKeys := Seq[BuildInfoKey](version, scalaBinaryVersion),
    buildInfoPackage := "com.soundcloud.twinagle.codegen",
    buildInfoUsePackageAsPath := true,
    publishLocal := publishLocal.dependsOn(runtime / publishLocal).value,
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false
  )

lazy val runtime = (project in file("runtime")).settings(
  commonSettings,
  name := "twinagle-runtime",
  crossScalaVersions := Seq(scala212, scala213, scala213dot13, scala3),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq("-rewrite", "-source:3.0-migration")
      case _ => Seq()
    }
  },
  libraryDependencies ++= {
    Seq(
      "com.twitter" %% "finagle-http" % "24.2.0" cross CrossVersion.for3Use2_13,
      "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.17",
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.1",
      "org.json4s" %% "json4s-native" % "4.0.7",
      "org.specs2" %% "specs2-core" % "4.20.5" % Test cross CrossVersion.for3Use2_13,
      "org.specs2" %% "specs2-mock" % "4.20.5" % Test cross CrossVersion.for3Use2_13
    )
  },
  // compile protobuf messages for unit tests
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  // specs2 tests have some dependencies that use partial functions that the scala pickler has issue with.
  // That's why we convert FailureToEliminateExistentialID (id=98) into info level
  Test / scalacOptions ++= Seq("-Wconf:id=98:i,any:v"),
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
