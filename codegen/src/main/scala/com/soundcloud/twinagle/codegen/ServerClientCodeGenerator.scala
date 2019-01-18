package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors._
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import protocbridge.Artifact
import scalapb.compiler.FunctionalPrinter
import scalapb.compiler.DescriptorImplicits
import scalapb.options.compiler.Scalapb

import scala.collection.JavaConverters._

object ServerClientCodeGenerator extends protocbridge.ProtocCodeGenerator {
  val params = scalapb.compiler.GeneratorParams()

  // This would make sbt-protoc append the following artifacts to the user's
  // project.  If you have a runtime library this is the place to specify it.
  override def suggestedDependencies: Seq[protocbridge.Artifact] = Seq(
    Artifact("com.soundcloud.twinagle", "twinagle-runtime_2.12", "0.1.0-SNAPSHOT"),
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

    val myFileGen = new FileGenerator(implicits)
    request.getFileToGenerateList.asScala.foreach { name =>
      val fileDesc = fileDescByName(name)
      val responseFile = myFileGen.generateFile(fileDesc)
      b.addFile(responseFile)
    }
    b.build.toByteArray
  }

  class FileGenerator(implicits: DescriptorImplicits) {

    import implicits._

    def generateFile(fileDesc: FileDescriptor): CodeGeneratorResponse.File = {
      val outputFile = CodeGeneratorResponse.File.newBuilder()
      outputFile.setName(
        s"${fileDesc.scalaDirectory}/Generated${fileDesc.fileDescriptorObjectName}.scala")

      val fp = FunctionalPrinter()
        // Add the header, including package name and imports
        .add(generateHeader(fileDesc))
        .print(fileDesc.getServices.asScala) { (p, m) =>
          p.add(generateServiceTrait(m))
            .add(generateClient(m))
            .add(generateServer(m))
        }
      outputFile.setContent(fp.result)
      outputFile.build
    }

    private def generateClient(serviceDescriptor: ServiceDescriptor) = {
      val clientName = getClientName(serviceDescriptor)
      val serviceName = getServiceName(serviceDescriptor)

      s"""
         |class ${clientName}Json(httpClient: Service[Request, Response]) extends $serviceName {
         |
         |${serviceDescriptor.methods.map(generateJsonClientMethod).mkString("\n")}
         |
         |}
       """.stripMargin

    }


    private def generateServiceTrait(serviceDescriptor: ServiceDescriptor): String = {
      val serviceName = getServiceName(serviceDescriptor)
      val methods = serviceDescriptor.methods
      s"""
         |trait $serviceName {
         |
         |${methods.map(getServiceMethodDeclaration).mkString("\n")}
         |}
     """.stripMargin
    }

    private def getServiceName(serviceDescriptor: ServiceDescriptor) = s"${serviceDescriptor.getName}Service"

    private def getServerName(serviceDescriptor: ServiceDescriptor) = s"${serviceDescriptor.getName}Server"

    private def getClientName(serviceDescriptor: ServiceDescriptor) = s"${serviceDescriptor.getName}Client"

    private def generateHeader(fileDesc: FileDescriptor) = {
      s"""package ${fileDesc.scalaPackageName}
         |
         |import java.net.InetSocketAddress
         |
         |import com.soundcloud.twinagle.{Endpoint, JsonConverter, Server, ServiceAdapter}
         |import com.twitter.util.Future
         |import com.twitter.finagle.{Http, Service, ServiceFactory}
         |import com.twitter.finagle.http.{Method, Request, Response}
         |import scalapb.json4s.JsonFormat
         |""".stripMargin
    }

    private def getServiceMethodDeclaration(methodDescriptor: MethodDescriptor) = {
      val varType = methodInputType(methodDescriptor)
      val varName = decapitalize(varType)
      val outputType = methodOutputType(methodDescriptor)
      val methodName = methodDescriptor.name
      s"  def $methodName($varName: $varType): Future[$outputType]"
    }


    private def methodInputType(methodDescriptor: MethodDescriptor) = {
      lastPart(methodDescriptor.inputType.scalaType)
    }

    private def methodOutputType(methodDescriptor: MethodDescriptor) = {
      lastPart(methodDescriptor.outputType.scalaType)
    }

    private def lastPart(str: String): String = str.lastIndexOf(".") match {
      case i if i > 0 => str.substring(i + 1)
      case _ => str
    }

    private def decapitalize(str: String) =
      if (str.length >= 1)
        str.substring(0, 1).toLowerCase() + str.substring(1)
      else str

    private def generateJsonClientMethod(methodDescriptor: MethodDescriptor) = {
      val varType = methodInputType(methodDescriptor)
      val varName = decapitalize(varType)
      val outputType = methodOutputType(methodDescriptor)
      val methodName = methodDescriptor.name
      val path = generatePathForMethod(methodDescriptor)
      s"""
         |  override def $methodName($varName: $varType): Future[$outputType] = {
         |    implicit val companion = $outputType.messageCompanion
         |    val converter = new JsonConverter()
         |    val request = converter.mkRequest($path, $varName)
         |    httpClient(request).map(x => converter.fromResponse(x))
         |  }""".stripMargin
    }

    private def generatePathForMethod(methodDescriptor: MethodDescriptor) = {
      s""""/twirp/${methodDescriptor.getService.getFullName}/${methodDescriptor.name.capitalize}""""
    }

    def getEndpoint(method: MethodDescriptor) =
      s"""|    Endpoint(${generatePathForMethod(method)}, new ServiceAdapter(service.${method.name}))""".stripMargin

    private def generateServer(serviceDescriptor: ServiceDescriptor) = {
      val serviceName = getServiceName(serviceDescriptor)
      val serverName = getServerName(serviceDescriptor)
      s"""
         |class $serverName(service: $serviceName, port: Int) {
         |  def build = {
         |    val endpoints = Seq(
         |    ${serviceDescriptor.methods.map(getEndpoint).mkString(",\n")}
         |    )
         |    Http.server.serve(new InetSocketAddress(port), ServiceFactory.const(new Server(endpoints)))
         |  }
         |}
         |
       """.stripMargin
    }

  }

}

