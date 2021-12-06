scalacOptions += "-Wconf:cat=deprecation:error"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.5")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")

libraryDependencies ++= Seq(
  "org.scala-sbt"        %% "scripted-plugin" % sbtVersion.value,
  "com.thesamet.scalapb" %% "compilerplugin"  % "0.11.6"
)
// only necessary so we can generate protos for tests
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4")
