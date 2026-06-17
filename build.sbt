import sbt.CrossVersion

lazy val scala212  = "2.12.20"
lazy val scala213  = "2.13.17"
lazy val scala3LTS = "3.3.7" // runtime stays on LTS — it is the end-user library
lazy val scala3    = "3.8.4" // codegen + plugin Scala-3 axis (matches sbt 2.0.0 DSL)
lazy val sbt2      = "2.0.0"
lazy val sbt1      = "1.9.9" // pluginCrossBuild sbt version for the 2.12 axis (oldest sbt 1.x we support)

lazy val commonSettings = List(
  scalaVersion := scala212,
  scalacOptions ++= {
    val common = Seq("-encoding", "utf8", "-deprecation", "-unchecked")
    CrossVersion.partialVersion(scalaVersion.value) match {
      // Scala 3 renamed -Xfatal-warnings to -Werror (the old alias is itself deprecated).
      case Some((2, _)) => common ++ Seq("-Xlint", "-Xfatal-warnings")
      case _            => common ++ Seq("-Werror")
    }
  },
  Compile / console / scalacOptions --= Seq("-deprecation", "-Xfatal-warnings", "-Werror", "-Xlint"),
  scalafmtOnCompile := true
)

lazy val codegen = (project in file("codegen"))
  .settings(
    commonSettings,
    name                                          := "twinagle-codegen",
    crossScalaVersions                            := Seq(scala212, scala213, scala3),
    libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion,
    publishLocal                                  := publishLocal.dependsOn(runtime / publishLocal).value
  )

lazy val plugin = (project in file("plugin"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .dependsOn(codegen)
  .settings(
    commonSettings,
    name               := "twinagle-scalapb-plugin",
    crossScalaVersions := Seq(scala212, scala3),
    scalaVersion       := scala212,
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => sbt1
        case _      => sbt2
      }
    },
    scriptedSbt := {
      scalaBinaryVersion.value match {
        case "2.12" => sbtVersion.value
        case _      => sbt2
      }
    },
    addSbtPlugin("com.thesamet"   % "sbt-protoc"  % "1.1.0-RC1"),
    addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.1.0"),
    buildInfoKeys             := Seq[BuildInfoKey](version, scalaBinaryVersion),
    buildInfoPackage          := "com.soundcloud.twinagle.plugin",
    buildInfoUsePackageAsPath := true,
    sbtPluginPublishLegacyMavenStyle := true,
    publishLocal              := publishLocal.dependsOn(runtime / publishLocal).value,
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
      "com.thesamet.scalapb" %% "scalapb-runtime" % "1.0.0-alpha.5",
      // scalapb-json4s has only published 1.0.0-alpha.1, which pulls scalapb-runtime alpha.1.
      // Exclude its transitive scalapb-runtime so only our alpha.5 above remains: sbt's
      // early-semver check otherwise flags the alpha.1 -> alpha.5 eviction as a hard conflict,
      // and a per-dependency exclude (unlike dependencyOverrides) serializes into the published
      // POM/ivy so downstream consumers inherit the coherent single version.
      ("com.thesamet.scalapb" %% "scalapb-json4s" % "1.0.0-alpha.1")
        .exclude("com.thesamet.scalapb", "scalapb-runtime_2.12")
        .exclude("com.thesamet.scalapb", "scalapb-runtime_2.13")
        .exclude("com.thesamet.scalapb", "scalapb-runtime_3"),
      "org.json4s"           %% "json4s-native"   % "4.0.7",
      "org.specs2"           %% "specs2-core"     % "4.20.9" % Test cross CrossVersion.for3Use2_13
    )
  },
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) | Some((2, 12)) =>
        Seq(
          "org.specs2" %% "specs2-mock" % "4.20.9" % Test
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
  .aggregate(runtime, codegen, plugin)
  .settings(
    name := "twinagle-root",
    resolvers += Resolver.typesafeIvyRepo("releases"),
    publish / skip := true
  )
