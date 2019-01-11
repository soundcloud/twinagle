package com.soundcloud.twinagle.codegen

import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.Descriptors._
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import protocbridge.Artifact

import scala.collection.JavaConverters._
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter}
import scalapb.options.compiler.Scalapb

/** This is the interface that code generators need to implement. */
object Generator extends protocbridge.ProtocCodeGenerator {
  val params = scalapb.compiler.GeneratorParams()

  // This would make sbt-protoc append the following artifacts to the user's
  // project.  If you have a runtime library this is the place to specify it.
  override def suggestedDependencies: Seq[protocbridge.Artifact] = Seq(
    Artifact("com.soundcloud.twinagle", "twinagle-runtime_2.12", "0.1.0-SNAPSHOT")
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

    val myFileGen = new FileGenerator(implicits)

    request.getFileToGenerateList.asScala.foreach {
      name =>
        val fileDesc = fileDescByName(name)
        val responseFile = myFileGen.generateFile(fileDesc)
        b.addFile(responseFile)
    }
    b.build.toByteArray
  }
}

class FileGenerator(implicits: DescriptorImplicits) {
  import implicits._

  def generateFile(fileDesc: FileDescriptor): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setName(s"${fileDesc.scalaDirectory}/${fileDesc.fileDescriptorObjectName}Foo.scala")
    val fp = FunctionalPrinter()
      .add(s"package ${fileDesc.scalaPackageName}")
      .add("")
      .print(fileDesc.getMessageTypes.asScala) {
        case (p, m) =>
          p.add(s"object ${m.getName}Boo {")
            .indent
            .add(s"type T = ${m.scalaTypeName}")
            .add(s"val FieldCount = ${m.getFields.size}")
            .outdent
            .add("}")
      }
      b.setContent(fp.result)
      b.build
  }
}
