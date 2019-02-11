package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors._
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import protocbridge.Artifact
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter}
import scalapb.options.compiler.Scalapb

import scala.collection.JavaConverters._

object ServerClientCodeGenerator extends protocbridge.ProtocCodeGenerator {
  val params = scalapb.compiler.GeneratorParams()

  // This would make sbt-protoc append the following artifacts to the user's
  // project.  If you have a runtime library this is the place to specify it.
  override def suggestedDependencies: Seq[protocbridge.Artifact] = Seq(
    Artifact("com.soundcloud.twinagle", s"twinagle-runtime_${BuildInfo.scalaBinaryVersion}", BuildInfo.version),
    Artifact("com.google.protobuf", "protobuf-java", "3.5.1")
  )

  def run(input: Array[Byte]): Array[Byte] = {
    val registry = ExtensionRegistry.newInstance()
    Scalapb.registerAllExtensions(registry)
    val request = CodeGeneratorRequest.parseFrom(input)
    val b = CodeGeneratorResponse.newBuilder

    val fileDescByName: Map[String, FileDescriptor] =
      request.getProtoFileList.asScala.foldLeft[Map[String, FileDescriptor]](Map.empty) {
        case (acc, fp) =>
          val deps = fp.getDependencyList.asScala.map(acc)
          acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
      }

    val implicits = new DescriptorImplicits(params, fileDescByName.values.toVector)

    val services = fileDescByName.values.flatMap(_.getServices.asScala)

    services.foreach { service =>
      val fileDesc = service.getFile

      import implicits._
      val twinaglePrinter = new TwinagleServicePrinter(service, implicits)
      val printer = twinaglePrinter.printService(FunctionalPrinter())

      val outputFile = CodeGeneratorResponse.File.newBuilder()
      outputFile.setName(
        s"${fileDesc.scalaDirectory}/Generated${service.getName}.scala")
      outputFile.setContent(printer.result())
      b.addFile(outputFile.build)
    }

    b.build.toByteArray
  }

}

