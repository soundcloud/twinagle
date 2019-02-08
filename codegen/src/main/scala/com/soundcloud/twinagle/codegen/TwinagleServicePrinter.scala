package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors.{FileDescriptor, MethodDescriptor, ServiceDescriptor}
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter}

import scala.collection.JavaConverters._

final class TwinagleServicePrinter(implicits: DescriptorImplicits) {

  import implicits._

  def generateServiceObject(m: ServiceDescriptor): String = {
    val serviceName = getServiceName(m)
    s"""
       |object $serviceName {
       |  def server(service: $serviceName): Service[Request, Response] = new Server(Seq(
       |${m.methods.map(generateEndpoint).mkString(",\n")}
       |  ))
       |}
       """.stripMargin
  }

  def generateEndpoint(md: MethodDescriptor): String = {
    val path = generatePathForMethod(md)
    s"    Endpoint($path, new ServiceAdapter(service.${decapitalize(md.getName)}))"
  }

  def generateFile(fileDesc: FileDescriptor): CodeGeneratorResponse.File = {
    val outputFile = CodeGeneratorResponse.File.newBuilder()
    outputFile.setName(
      s"${fileDesc.scalaDirectory}/Generated${fileDesc.fileDescriptorObjectName}.scala")

    val fp = FunctionalPrinter()
      // Add the header, including package name and imports
      .add(generateHeader(fileDesc))
      .print(fileDesc.getServices.asScala) { (p, m) =>
        p.add(generateServiceTrait(m))
          .add(generateServiceObject(m))
          .add(generateJsonClient(m))
          .add(generateProtobufClient(m))
          .add(generateServer(m))
      }
    outputFile.setContent(fp.result)
    outputFile.build
  }

  private def generateJsonClient(serviceDescriptor: ServiceDescriptor) = {
    val clientName = getClientName(serviceDescriptor)
    val serviceName = getServiceName(serviceDescriptor)

    s"""
       |class ${clientName}Json(httpClient: Service[Request, Response]) extends $serviceName {
       |
         |${serviceDescriptor.methods.map(generateJsonClientService).mkString("\n")}
       |${serviceDescriptor.methods.map(generateGenericClientMethod).mkString("\n")}
       |}
       """.stripMargin

  }

  private def generateProtobufClient(serviceDescriptor: ServiceDescriptor) = {
    val clientName = getClientName(serviceDescriptor)
    val serviceName = getServiceName(serviceDescriptor)

    s"""
       |class ${clientName}Protobuf(httpClient: Service[Request, Response]) extends $serviceName {
       |
         |${serviceDescriptor.methods.map(generateProtobufClientService).mkString("\n")}
       |${serviceDescriptor.methods.map(generateGenericClientMethod).mkString("\n")}
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
         |import com.soundcloud.twinagle._
       |import com.twitter.finagle.{Http, Service, ServiceFactory}
       |import com.twitter.finagle.http.{Request, Response}
       |import com.twitter.util.Future
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

  private def generateJsonClientService(methodDescriptor: MethodDescriptor): String = {
    val varType = methodInputType(methodDescriptor)
    val varName = decapitalize(varType)
    val inputType = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = methodDescriptor.name
    val path = generatePathForMethod(methodDescriptor)
    s"""
       |  private val ${methodName}Service: Service[$inputType, $outputType] = {
       |    implicit val companion = $outputType
       |    new JsonClientFilter($path) andThen
       |      new TwirpHttpClient andThen
       |      httpClient
       |  }
      """.stripMargin
  }

  private def generateProtobufClientService(methodDescriptor: MethodDescriptor): String = {
    val varType = methodInputType(methodDescriptor)
    val varName = decapitalize(varType)
    val inputType = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = methodDescriptor.name
    val path = generatePathForMethod(methodDescriptor)
    s"""
       |  private val ${methodName}Service: Service[$inputType, $outputType] = {
       |    implicit val companion = $outputType
       |    new ProtobufClientFilter($path) andThen
       |      new TwirpHttpClient andThen
       |      httpClient
       |  }
      """.stripMargin
  }

  private def generateGenericClientMethod(methodDescriptor: MethodDescriptor) = {
    val inputType = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = methodDescriptor.name
    s"""
       |  override def $methodName(request: $inputType): Future[$outputType] = ${methodName}Service(request)
       """.stripMargin
  }

  private def generatePathForMethod(methodDescriptor: MethodDescriptor) = {
    s""""/twirp/${methodDescriptor.getService.getFullName}/${methodDescriptor.name.capitalize}""""
  }

  private def generateServer(serviceDescriptor: ServiceDescriptor) = {
    val serviceName = getServiceName(serviceDescriptor)
    val serverName = getServerName(serviceDescriptor)
    s"""
       |class $serverName(service: $serviceName, port: Int) {
       |  def build = {
       |    val server = $serviceName.server(service)
       |    Http.server.serve(new InetSocketAddress(port), ServiceFactory.const(server))
       |  }
       |}
       |
       """.stripMargin
  }

}
