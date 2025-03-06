package com.soundcloud.twinagle.plugin

import protocbridge.{JvmGenerator, Target}
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB

object Twinagle extends AutoPlugin {
  val scalapbCodeGeneratorOptions = settingKey[Set[scalapb.GeneratorOption]]("Settings for scalapb code generation")

  override def requires: Plugins = sbtprotoc.ProtocPlugin && JvmPlugin

  override def trigger: PluginTrigger = NoTrigger

  override def projectSettings: Seq[Def.Setting[_]] = List(
    scalapbCodeGeneratorOptions := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          Set(
            scalapb.GeneratorOption.FlatPackage, // don't include proto filename in scala package name
            scalapb.GeneratorOption.Scala3Sources
          )
        case _ =>
          Set(
            scalapb.GeneratorOption.FlatPackage // don't include proto filename in scala package name
          )
      }
    },
    Compile / PB.targets := Seq(
      Target(
        scalapb.gen(scalapbCodeGeneratorOptions.value - scalapb.GeneratorOption.Grpc),
        (Compile / sourceManaged).value / "twinagle-protobuf"
      ),
      Target(
        JvmGenerator("scala-twinagle", SbtServerClientCodeGenerator),
        (Compile / sourceManaged).value / "twinagle-services",
        scalapb.gen(scalapbCodeGeneratorOptions.value)._2
      )
    ),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    ),
    excludeDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => Seq("org.scala-lang.modules" % "scala-collection-compat_2.13")
        case _            => Seq.empty
      }
    }
  )
}
