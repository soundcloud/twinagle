scalacOptions += "-Wconf:cat=deprecation:error"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.5")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.2")

libraryDependencies ++= Seq(
  "org.scala-sbt"        %% "scripted-plugin" % sbtVersion.value,
  "com.thesamet.scalapb" %% "compilerplugin"  % "0.11.20"
)
// only necessary so we can generate protos for tests
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
