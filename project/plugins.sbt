addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.3")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
  "com.thesamet.scalapb" %% "compilerplugin" % "0.8.4"
)
