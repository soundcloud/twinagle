addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1-M3")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
  "com.thesamet.scalapb" %% "compilerplugin" % "0.10.0-M4"
)
