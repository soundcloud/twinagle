package com.soundcloud.protoize

import sbt.Keys.{libraryDependencies, sourceManaged}
import sbt._
import sbtprotoc.ProtocPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB

object ProtoPlugin extends AutoPlugin {
  override def requires = sbtprotoc.ProtocPlugin

  PB.targets in Compile := Seq(
    scalapb.gen(grpc=false) -> (sourceManaged in Compile).value,
    ServerClientCodeGenerator -> (sourceManaged in Compile).value
  )

  // For SBT protoc compiler plugin
  libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.5.1"
  libraryDependencies += "com.google.protobuf" % "protobuf-java-util" % "3.5.1" % ProtocPlugin.ProtobufConfig.name // for things like Timestamps (https://github.com/protocolbuffers/protobuf/tree/master/java/util/src/main/java/com/google/protobuf/util)
  libraryDependencies += "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.0"

}
