package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors._
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import protocbridge.Artifact
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter, GeneratorException, ProtobufGenerator}
import scalapb.options.compiler.Scalapb

import scala.collection.JavaConverters._

object ServerClientCodeGenerator extends protocbridge.ProtocCodeGenerator {
  val params = scalapb.compiler.GeneratorParams()

  // This would make sbt-protoc append the following artifacts to the user's
  // project.  If you have a runtime library this is the place to specify it.
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

  def handleCodeGeneratorRequest(
      request: PluginProtos.CodeGeneratorRequest
  ): PluginProtos.CodeGeneratorResponse = {
    val b = CodeGeneratorResponse.newBuilder
    ProtobufGenerator.parseParameters(request.getParameter) match {
      case Right(params) =>
        try {
          val filesByName: Map[String, FileDescriptor] =
            request.getProtoFileList.asScala
              .foldLeft[Map[String, FileDescriptor]](Map.empty) {
                case (acc, fp) =>
                  val deps = fp.getDependencyList.asScala.map(acc)
                  acc + (fp.getName -> FileDescriptor.buildFrom(
                    fp,
                    deps.toArray
                  ))
              }

          val implicits =
            new DescriptorImplicits(params, filesByName.values.toVector)
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
