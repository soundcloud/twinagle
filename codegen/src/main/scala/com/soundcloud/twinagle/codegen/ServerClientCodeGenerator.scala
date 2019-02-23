package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors._
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import com.soundcloud.twinagle.codegen.TwinaglePlugin.autoImport.scalapbCodeGenerators
import protocbridge.{Artifact, JvmGenerator, Target}
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter, GeneratorException, ProtobufGenerator}
import scalapb.options.compiler.Scalapb

import scala.collection.JavaConverters._


sealed trait CodeGeneratorOption extends Product with Serializable

object ServerClientCodeGenerator extends protocbridge.ProtocCodeGenerator {
  val params = scalapb.compiler.GeneratorParams()

  // This would make sbt-protoc append the following artifacts to the user's
  // project.  If you have a runtime library this is the place to specify it.
  override def suggestedDependencies: Seq[protocbridge.Artifact] = Seq(
    Artifact("com.soundcloud.twinagle", s"twinagle-runtime_${BuildInfo.scalaBinaryVersion}", BuildInfo.version),
    Artifact("com.google.protobuf", "protobuf-java", "3.5.1")
  )

  def generateServiceFiles(file: FileDescriptor,
                           di: DescriptorImplicits): Seq[PluginProtos.CodeGeneratorResponse.File] = {
    file.getServices.asScala.map { service =>
      val p = new TwinagleServicePrinter(service, di)

      import di.{FileDescriptorPimp, ServiceDescriptorPimp}
      val code = p.printService(FunctionalPrinter()).result()
      val b = CodeGeneratorResponse.File.newBuilder()
      b.setName(file.scalaDirectory + "/" + service.name + ".scala")
      b.setContent(code)
      println(b.getName)
      b.build
    }
  }

  def handleCodeGeneratorRequest(request: PluginProtos.CodeGeneratorRequest): PluginProtos.CodeGeneratorResponse = {
    val b = CodeGeneratorResponse.newBuilder
    ProtobufGenerator.parseParameters(request.getParameter) match {
      case Right(params) =>
        try {

          val filesByName: Map[String, FileDescriptor] =
            request.getProtoFileList.asScala.foldLeft[Map[String, FileDescriptor]](Map.empty) {
              case (acc, fp) =>
                val deps = fp.getDependencyList.asScala.map(acc)
                acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
            }

          val implicits = new DescriptorImplicits(params, filesByName.values.toVector)
          val genFiles = request.getFileToGenerateList.asScala.map(filesByName)
          val srvFiles = genFiles.flatMap(generateServiceFiles(_, implicits))
          b.addAllFile(srvFiles.asJava)
        } catch {
          case e: GeneratorException =>
            b.setError(e.message)
        }

      case Left(error) =>
        b.setError(error)
    }

    b.build()
  }

  override def run(req: Array[Byte]): Array[Byte] = {
    println("Running")
    val registry = ExtensionRegistry.newInstance()
    Scalapb.registerAllExtensions(registry)
    val request = CodeGeneratorRequest.parseFrom(req, registry)
    handleCodeGeneratorRequest(request).toByteArray
  }

}

object Twinagle extends AutoPlugin {
  override def requires: Plugins = TwinaglePlugin

  override def trigger: PluginTrigger = NoTrigger

  override def projectSettings: Seq[Def.Setting[_]] = List(
    PB.targets := scalapbCodeGenerators.value
  )
}

object TwinaglePlugin extends AutoPlugin {

  object autoImport {

    object CodeGeneratorOption {

      case object FlatPackage extends CodeGeneratorOption {
        override def toString = "flat_package"
      }

      case object JavaConversions extends CodeGeneratorOption {
        override def toString: String = "java_conversions"
      }

      case object Twinagle extends CodeGeneratorOption {
        override def toString: String = "twinagle"
      }

      case object SingleLineToProtoString extends CodeGeneratorOption {
        override def toString: String = "single_line_to_proto_string"
      }

      case object AsciiFormatToString extends CodeGeneratorOption {
        override def toString: String = "ascii_format_to_string"
      }

    }

    val scalapbCodeGeneratorOptions =
      settingKey[Seq[CodeGeneratorOption]]("Settings for scalapb/fs2-grpc code generation")
    val scalapbProtobufDirectory =
      settingKey[File]("Directory containing protobuf files for scalapb")
    val scalapbCodeGenerators =
      settingKey[Seq[Target]]("Code generators for scalapb")

  }

  import autoImport._

  override def requires = sbtprotoc.ProtocPlugin && JvmPlugin

  override def trigger = NoTrigger

  def convertOptionsToScalapbGen(options: Set[CodeGeneratorOption]): (JvmGenerator, Seq[String]) = {
    scalapb.gen(
      flatPackage = options(CodeGeneratorOption.FlatPackage),
      javaConversions = options(CodeGeneratorOption.JavaConversions),
      grpc = false,
      singleLineToProtoString = options(CodeGeneratorOption.SingleLineToProtoString),
      asciiFormatToString = options(CodeGeneratorOption.AsciiFormatToString)
    )
  }

  override def projectSettings: Seq[Def.Setting[_]] = List(
    scalapbProtobufDirectory := (sourceManaged in Compile).value,
    scalapbCodeGenerators := {
      Target(convertOptionsToScalapbGen(scalapbCodeGeneratorOptions.value.toSet), (sourceManaged in Compile).value) ::
        Option(
          Target(
            (JvmGenerator("scala-twinagle", ServerClientCodeGenerator),
              scalapbCodeGeneratorOptions.value.filterNot(_ == CodeGeneratorOption.Twinagle).map(_.toString)),
            (sourceManaged in Compile).value
          ))
          .filter(_ => scalapbCodeGeneratorOptions.value.contains(CodeGeneratorOption.Twinagle))
          .toList
    },
    scalapbCodeGeneratorOptions := Seq(CodeGeneratorOption.Twinagle),
    libraryDependencies ++= List(
      "com.soundcloud.twinagle" %% "twinagle-runtime" % com.soundcloud.twinagle.codegen.BuildInfo.version,
      // not necessary? gets pulled in by twinagle-runtime
      //      "com.thesamet.scalapb"    %% "scalapb-json4s"       % "0.7.2",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion
    )
  )
}
