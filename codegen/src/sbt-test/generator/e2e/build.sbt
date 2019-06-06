enablePlugins(Twinagle)

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Xfatal-warnings"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
