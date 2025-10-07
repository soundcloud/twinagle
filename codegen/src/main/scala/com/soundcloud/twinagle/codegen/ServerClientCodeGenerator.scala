package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors._
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import protocgen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter, ProtobufGenerator}

import scala.jdk.CollectionConverters._

trait ServerClientCodeGenerator extends CodeGenApp {
  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    scalapb.options.Scalapb.registerAllExtensions(registry)
  }

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
          } yield serviceFile,
          supportedFeatures = Set(CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL)
        )
      case Left(error) =>
        CodeGenResponse.fail(error)
    }

  def generateServiceFiles(
      file: FileDescriptor,
      di: DescriptorImplicits
  ): Seq[PluginProtos.CodeGeneratorResponse.File] = {
    file.getServices.asScala.toSeq.map { service =>
      val p = new TwinagleServicePrinter(service, di)

      import di.{ExtendedFileDescriptor, ExtendedServiceDescriptor}
      val code = p.printService(FunctionalPrinter()).result()
      val b    = CodeGeneratorResponse.File.newBuilder()
      b.setName(file.scalaDirectory + "/" + service.name + ".scala")
      b.setContent(code)
      b.build
    }
  }

}

object StandaloneServerClientCodeGenerator extends ServerClientCodeGenerator {}
