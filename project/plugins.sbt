addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.3")

libraryDependencies ++= Seq(
  "org.scala-sbt"        %% "scripted-plugin" % sbtVersion.value,
  "com.thesamet.scalapb" %% "compilerplugin"  % "0.10.6"
)
// only necessary so we can generate protos for tests
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.32")
