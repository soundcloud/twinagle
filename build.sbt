import sbt.CrossVersion

lazy val scala212  = "2.12.18"
lazy val scala213  = "2.13.16"
lazy val scala3LTS = "3.3.5"

lazy val commonSettings = List(
  scalaVersion := scala212,
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => scalacOptions ++= Seq("-Xlint")
      case _            => ()
    }
    Seq(
      "-encoding",
      "utf8",
      "-deprecation",
      "-unchecked",
      "-Xfatal-warnings"
    )
  },
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
  crossScalaVersions := Seq(scala212, scala213, scala3LTS),
  // finagle uses 2.13 heavily so we will ignore our project runtime compat
  excludeDependencies += "org.scala-lang.modules" % "scala-collection-compat_3",
  libraryDependencies ++= {
    Seq(
      "com.twitter"          %% "finagle-http"    % "24.2.0" cross CrossVersion.for3Use2_13,
      "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.17",
      "com.thesamet.scalapb" %% "scalapb-json4s"  % "0.12.1",
      "org.json4s"           %% "json4s-native"   % "4.0.7",
      "org.specs2"           %% "specs2-core"     % "4.20.8" % Test cross CrossVersion.for3Use2_13
    )
  },
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) | Some((2, 12)) =>
        Seq(
          "org.specs2" %% "specs2-mock" % "4.20.8" % Test
        )
      case Some((3, 3)) =>
        Seq(
          "org.scalamock" %% "scalamock" % "6.1.1" % Test
        )
      case _ => Seq.empty
    }
  },
  // compile protobuf messages for unit tests
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  Test / PB.targets := {
    val gen3 = CrossVersion.partialVersion(scalaVersion.value).exists(a => a._1 == 3L)
    Seq(
      scalapb.gen(flatPackage = true, scala3Sources = gen3) -> (Test / sourceManaged).value
    )
  }
)

lazy val root = (project in file("."))
  .aggregate(runtime, codegen)
  .settings(
    name := "twinagle-root",
    resolvers += Resolver.typesafeIvyRepo("releases"),
    publish / skip := true
  )
