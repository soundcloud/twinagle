enablePlugins(Twinagle)

scalacOptions ++= Seq(
  "-encoding",
  "utf8",
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Xfatal-warnings"
)

libraryDependencies += "org.specs2" %% "specs2-core" % "4.12.3" % Test
