lazy val scala212 = "2.12.18"
lazy val scala213 = "2.13.7"
lazy val scala213dot11 = "2.13.14"
lazy val scala3 = "3.3.4"

lazy val commonSettings = List(
  scalaVersion := scala212,
  scalacOptions ++= {
    val x = CrossVersion.partialVersion(scalaVersion.value) flatMap {
      case (2, _) => Some(Seq("-Xlint"))
      case _ => None
    }
    Seq(
      "-encoding",
      "utf8",
      "-deprecation",
      "-unchecked",
      // "-Xfatal-warnings"
    ) ++ x.getOrElse(Seq.empty)
  },
  excludeDependencies += "org.scala-lang.modules" % "scala-collection-compat_2.13",
  Compile / console / scalacOptions --= {
    val x = CrossVersion.partialVersion(scalaVersion.value) flatMap {
      case (2, _) => Some(Seq("-Xlint"))
      case _ => None
    }
    Seq("-deprecation" /*, "-Xfatal-warnings" */) ++ x.getOrElse(Seq.empty)
  }
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
  crossScalaVersions := Seq(scala212, scala213, scala213dot11, scala3),
  libraryDependencies ++= {
    Seq(
      "com.twitter" %% "finagle-http" % "24.2.0" cross CrossVersion.for3Use2_13,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion cross CrossVersion.for3Use2_13,
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.1" cross CrossVersion.for3Use2_13,
      "org.json4s" %% "json4s-native" % "4.1.0-M8" cross CrossVersion.for3Use2_13,
      "org.specs2" %% "specs2-core" % "4.20.5" cross CrossVersion.for3Use2_13,
      "org.specs2" %% "specs2-mock" % "4.20.5" % Test cross CrossVersion.for3Use2_13
    )
  },
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
