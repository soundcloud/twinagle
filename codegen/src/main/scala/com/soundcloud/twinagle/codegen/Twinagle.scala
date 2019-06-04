package com.soundcloud.twinagle.codegen

import protocbridge.{JvmGenerator, Target}
import sbtprotoc.ProtocPlugin.autoImport.PB
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import sbtprotoc.ProtocPlugin.ProtobufConfig

object Twinagle extends AutoPlugin {
  override def requires: Plugins = sbtprotoc.ProtocPlugin && JvmPlugin

  override def trigger: PluginTrigger = NoTrigger

  override def projectSettings: Seq[Def.Setting[_]] = List(
    PB.targets := Seq(
      Target(scalapb.gen(grpc = false), (sourceManaged in Compile).value),
      Target(JvmGenerator("scala-twinagle", ServerClientCodeGenerator), (sourceManaged in Compile).value)
    ),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    )
  )
}
