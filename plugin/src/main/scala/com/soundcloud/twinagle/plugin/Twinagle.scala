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

  // Return type is inherited from AutoPlugin.projectSettings. We avoid writing it
  // explicitly because sbt 1 (Scala 2.12) types it as Seq[Setting[_]] while sbt 2
  // (Scala 3) uses Seq[Setting[?]], and `_` is a fatal deprecation under -Werror on 3.x.
  override def projectSettings = List(
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
      // ExclusionRule directly (rather than `org % name`): the implicit
      // OrganizationArtifactName -> InclExclRule conversion sbt 1 relied on here is
      // gone in sbt 2 / Scala 3. The _2.13 suffix is the literal artifact name finagle
      // drags in via for3Use2_13, not a cross-versioned suffix.
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          Seq(ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"))
        case _ => Seq.empty
      }
    }
  )
}
