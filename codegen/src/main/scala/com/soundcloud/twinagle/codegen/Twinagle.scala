package com.soundcloud.twinagle.codegen

import protocbridge.{JvmGenerator, Target}
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB

object Twinagle extends AutoPlugin {
  val scalapbCodeGeneratorOptions = settingKey[Set[scalapb.GeneratorOption]]("Settings for scalapb code generation")

  override def requires: Plugins = sbtprotoc.ProtocPlugin && JvmPlugin

  override def trigger: PluginTrigger = NoTrigger

  override def projectSettings: Seq[Def.Setting[_]] = List(
    scalapbCodeGeneratorOptions := Set(
      scalapb.GeneratorOption.FlatPackage // don't include proto filename in scala package name
    ),
    Compile / PB.targets := Seq(
      Target(
        scalapb.gen(scalapbCodeGeneratorOptions.value - scalapb.GeneratorOption.Grpc),
        (Compile / sourceManaged).value
      ),
      Target(
        JvmGenerator("scala-twinagle", ServerClientCodeGenerator),
        (Compile / sourceManaged).value,
        scalapb.gen(scalapbCodeGeneratorOptions.value)._2
      )
    ),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    )
  )
}
