name := "protoize"

version := "0.1"
sbtPlugin := true
organization := "com.soundcloud"
scalaVersion := "2.12.7"

// sbt-protoc for generating code from Protobuf
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.15")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0" // Must be here because library is used before compile stage

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.10"
libraryDependencies += "org.specs2" %% "specs2-core" % "4.3.4"
