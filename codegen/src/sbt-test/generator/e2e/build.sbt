import sbt.Compile

lazy val e2e = (project in file("."))
    .enablePlugins(Twinagle)
    .settings(
      // test that customizing the options works
      Twinagle.scalapbCodeGeneratorOptions += scalapb.GeneratorOption.JavaConversions,
      Compile / PB.targets += PB.gens.java -> (sourceManaged in Compile).value,
      scalacOptions ++= Seq(
        "-encoding", "utf8",
        "-deprecation",
        "-unchecked",
        "-Xlint",
        "-Xfatal-warnings"
      ),
      libraryDependencies += "org.specs2" %% "specs2-core" % "4.10.0" % Test
    )
