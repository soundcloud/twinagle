package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors._
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import protocbridge.Artifact
import protocgen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter, ProtobufGenerator}

import scala.collection.JavaConverters._

object ServerClientCodeGenerator extends CodeGenApp {
  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    scalapb.options.Scalapb.registerAllExtensions(registry)
  }

  override def suggestedDependencies: Seq[protocbridge.Artifact] = Seq(
    Artifact(
      "com.soundcloud",
      "twinagle-runtime",
      BuildInfo.version,
      crossVersion = true
    ),
    Artifact(
      "com.thesamet.scalapb",
      "scalapb-runtime",
      scalapb.compiler.Version.scalapbVersion,
      crossVersion = true
    )
  )

  def process(request: CodeGenRequest): CodeGenResponse =
    ProtobufGenerator.parseParameters(request.parameter) match {
      case Right(params) =>
        // Implicits gives you extension methods that provide ScalaPB names and types
        // for protobuf entities.
        val implicits = DescriptorImplicits.fromCodeGenRequest(params, request)

        // Process each top-level message in each file.
        // This can be customized if you want to traverse the input in a different way.
        CodeGenResponse.succeed(
          for {
            file        <- request.filesToGenerate
            serviceFile <- generateServiceFiles(file, implicits)
          } yield serviceFile
        )
      case Left(error) =>
        CodeGenResponse.fail(error)
    }

  def generateServiceFiles(
      file: FileDescriptor,
      di: DescriptorImplicits
  ): Seq[PluginProtos.CodeGeneratorResponse.File] = {
    file.getServices.asScala.map { service =>
      val p = new TwinagleServicePrinter(service, di)

      import di.{FileDescriptorPimp, ServiceDescriptorPimp}
      val code = p.printService(FunctionalPrinter()).result()
      val b    = CodeGeneratorResponse.File.newBuilder()
      b.setName(file.scalaDirectory + "/" + service.name + ".scala")
      b.setContent(code)
      println(b.getName)
      b.build
    }
  }

}
