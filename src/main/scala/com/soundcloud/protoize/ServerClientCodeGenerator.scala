package com.soundcloud.protoize

import com.google.protobuf.Descriptors._
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import scalapb.compiler.{DescriptorPimps, FunctionalPrinter}

import scala.collection.JavaConverters._

object ServerClientCodeGenerator extends protocbridge.ProtocCodeGenerator with DescriptorPimps {
  val params = scalapb.compiler.GeneratorParams()

  def run(input: Array[Byte]): Array[Byte] = {
    val request = CodeGeneratorRequest.parseFrom(input)
    val b = CodeGeneratorResponse.newBuilder

    val fileDescByName: Map[String, FileDescriptor] =
      request.getProtoFileList.asScala
        .foldLeft[Map[String, FileDescriptor]](Map.empty) {
          case (acc, fp) =>
            val deps = fp.getDependencyList.asScala.map(acc)
            acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
        }

    request.getFileToGenerateList.asScala.foreach { name =>
      val fileDesc = fileDescByName(name)
      val responseFile = generateFile(fileDesc)
      b.addFile(responseFile)
    }
    b.build.toByteArray
  }

  def generateFile(fileDesc: FileDescriptor): CodeGeneratorResponse.File = {
    val outputFile = CodeGeneratorResponse.File.newBuilder()
    outputFile.setName(
      s"${fileDesc.scalaDirectory}/Generated${fileDesc.fileDescriptorObjectName}.scala")

    val fp = FunctionalPrinter()
    // Add the header, including package name and imports
      .add(generateHeader(fileDesc))
      .print(fileDesc.getServices.asScala)(printServiceTrait) // Service class
      .print(fileDesc.getServices.asScala)(printHandlerRoutes) // HandlerRouterBuilder for handling generated clients
      .print(fileDesc.getServices.asScala)(printAbstractControllerClass) // Trait for the Controller, which handles HTTP requests and Responses
      .print(fileDesc.getServices.asScala)(printDefaultControllerClass) // Controller class for handling auto-generated clients
      .print(fileDesc.getServices.asScala)(printConverterTrait) // Converter trait for turning HTTP objects into RPC objects
      .print(fileDesc.getServices.asScala)(printDefaultConverterClass) // Default converter implementation for auto-generated clients
      .print(fileDesc.getServices.asScala)(printClient)

    outputFile.setContent(fp.result)
    outputFile.build
  }

  private def printClient(p: FunctionalPrinter,
                          serviceDescriptor: ServiceDescriptor) = {
    val clientName = getClientName(serviceDescriptor)
    p.add(s"class $clientName(jsonClient: JsonClient) {")
      .add("")
      .indent
      .print(serviceDescriptor.methods)(printClientMethodDeclaration)
      .outdent
      .add("}")
  }

  private def printConverterTrait(p: FunctionalPrinter,
                                  serviceDescriptor: ServiceDescriptor) = {
    val converterName = getConverterClassName(serviceDescriptor)
    p.add(s"trait $converterName {")
      .add("")
      .indent
      .print(serviceDescriptor.methods)(printConverterMethodDef)
      .outdent
      .add("}")
  }

  private def printDefaultConverterClass(
      p: FunctionalPrinter,
      serviceDescriptor: ServiceDescriptor) = {
    val converterTraitName = getConverterClassName(serviceDescriptor)
    p.add(s"class Default$converterTraitName extends $converterTraitName{")
      .add("")
      .indent
      .print(serviceDescriptor.methods)(printDefaultConverterMethod)
      .outdent
      .add("}")
  }

  private def getConverterClassName(serviceDescriptor: ServiceDescriptor) = {
    s"${serviceDescriptor.getName}Converter"
  }

  private def printDefaultControllerClass(
      p: FunctionalPrinter,
      serviceDescriptor: ServiceDescriptor) = {
    val defaultControllerName = generateDefaultControllerName(serviceDescriptor)
    val serviceName = getServiceName(serviceDescriptor)
    val converterClass = getConverterClassName(serviceDescriptor)
    val abstractControllerName = generateAbstractControllerName(
      serviceDescriptor)
    p.add(s"""
           |class $defaultControllerName(rpc: $serviceName, converter: $converterClass)
           |  extends $abstractControllerName(rpc, converter) {""".stripMargin)
      .add("")
      .indent
      .print(serviceDescriptor.methods)(printDefaultControllerMethod)
      .outdent
      .add("}")
  }

  private def printHandlerRoutes(p: FunctionalPrinter,
                                 serviceDescriptor: ServiceDescriptor) = {
    val controllerName = generateAbstractControllerName(serviceDescriptor)

    def registerLines = serviceDescriptor.methods.map(defineRouteRegistration).mkString("\n")

    val s =
      s"""
         |object ${serviceDescriptor.getName}HandlerRoutes {
         |
         |  def addRoutes(controller: $controllerName) = HandlerRouterBuilder
         |    $registerLines
         |}
       """.stripMargin

    p.add(s)
  }

  private def printServiceTrait(p: FunctionalPrinter, serviceDescriptor: ServiceDescriptor) = {
    val serviceName = getServiceName(serviceDescriptor)
    p
      .add(s"trait $serviceName {")
      .add("")
      .indent
      .print(serviceDescriptor.methods)(printServiceMethodDeclaration)
      .outdent
      .add("}")
  }

  private def getServiceName(serviceDescriptor: ServiceDescriptor) = {
    s"${serviceDescriptor.getName}Service"
  }

  private def getClientName(serviceDescriptor: ServiceDescriptor) = {
    s"${serviceDescriptor.getName}Client"
  }

  private def generateHeader(fileDesc: FileDescriptor) = {
    s"""package ${fileDesc.scalaPackageName}
       |
       |import com.soundcloud.jvmkit.module.http.client.JsonClient
       |import com.twitter.util.Future
       |import com.soundcloud.jvmkit.module.http.server.{HandlerRequest, HandlerRouterBuilder, JsonResponseBuilder}
       |import com.soundcloud.jvmkit.module.util.Path
       |import com.twitter.finagle.http.{Method, Response}
       |import scalapb.json4s.JsonFormat
       |
        |""".stripMargin
  }

  private def printServiceMethodDeclaration(p: FunctionalPrinter, methodDescriptor: MethodDescriptor) = {
    val varType = methodInputType(methodDescriptor)
    val varName = decapitalize(varType)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = methodDescriptor.name
    val method = s"def $methodName($varName: $varType): Future[$outputType]"
    p
      .add(method)
      .add("")
  }



  private def methodInputType(methodDescriptor: MethodDescriptor) = {
    lastPart(methodDescriptor.scalaIn)
  }

  private def methodOutputType(methodDescriptor: MethodDescriptor) = {
    lastPart(methodDescriptor.scalaOut)
  }

  private def lastPart(str: String): String = str.lastIndexOf(".") match {
    case i if i > 0 => str.substring(i + 1)
    case _          => str
  }

  private def decapitalize(str: String) =
    if (str.length > 1)
      str.substring(0, 1).toLowerCase() + str.substring(1)
    else str

  private def printClientMethodDeclaration(p: FunctionalPrinter, methodDescriptor: MethodDescriptor) = {
    val varType = methodInputType(methodDescriptor)
    val varName = decapitalize(varType)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = methodDescriptor.name
    val path = generatePathForMethod(methodDescriptor)
    val methodBody =
      s"""def $methodName($varName: $varType): Future[$outputType] = {
         |    jsonClient.post(
         |      path = Path($path),
         |      body = Some(JsonFormat.toJsonString($varName))
         |    ).map { r =>
         |      JsonFormat.fromJsonString[$outputType](r.contentString)
         |    }
         |  }""".stripMargin
    p.add(methodBody)
      .add("")
  }

  private def printRouteVariableForMethod(
      p: FunctionalPrinter,
      methodDescriptor: MethodDescriptor) = {
    val methodName = methodDescriptor.name.capitalize
    val path = generatePathForMethod(methodDescriptor)
    val str =
      s"""val ${methodName}Path: String = $path"""
    p.add(str)
  }

  private def generatePathForMethod(methodDescriptor: MethodDescriptor) = {
    s""""/rpc/${methodDescriptor.getService.getFullName}/${methodDescriptor.name.capitalize}""""
  }

  private def printRouteRegistration(p: FunctionalPrinter,
                                     methodDescriptor: MethodDescriptor) = {
    val value: String = defineRouteRegistration(methodDescriptor)
    p.add(value)
  }

  private def defineRouteRegistration(
      methodDescriptor: MethodDescriptor): String = {
    val path = generatePathForMethod(methodDescriptor)
    s""".register(Method.Post, $path, controller.${controllerMethodName(methodDescriptor)})"""
  }

  private def controllerMethodName(m: MethodDescriptor) =
    s"handle${m.name.capitalize}"

  private def printDefaultControllerMethod(
      printer: FunctionalPrinter,
      methodDescriptor: MethodDescriptor) = {
    val controllerMethod = controllerMethodName(methodDescriptor)
    val rpcMethodName = methodDescriptor.name
    printer
      .add(s"def $controllerMethod(handlerRequest: HandlerRequest): Future[Response] = {")
      .indent
      .add(s"val req = converter.${converterMethodName(methodDescriptor)}(handlerRequest)")
      .add("")
      .add(s"rpc.$rpcMethodName(req)")
      .indent
      .add(".map(JsonFormat.toJsonString)")
      .add(".map(JsonResponseBuilder.ok)")
      .outdent
      .outdent
      .add("}")
  }

  private def generateDefaultControllerName(m: ServiceDescriptor) =
    s"Default${m.getName}Controller"

  private def converterMethodName(m: MethodDescriptor) =
    s"convertTo${methodInputType(m)}"

  private def printDefaultConverterMethod(
      printer: FunctionalPrinter,
      methodDescriptor: MethodDescriptor) = {
    val rpcMethodInputType = methodInputType(methodDescriptor)
    val methodName = converterMethodName(methodDescriptor)
    printer
      .add(
        s"def $methodName(handlerRequest: HandlerRequest): $rpcMethodInputType = {")
      .indent
      .add(
        s"JsonFormat.fromJsonString[$rpcMethodInputType](handlerRequest.contentString)")
      .outdent
      .add("}")
  }

  private def printConverterMethodDef(printer: FunctionalPrinter,
                                      methodDescriptor: MethodDescriptor) = {
    val rpcMethodInputType = methodInputType(methodDescriptor)
    val methodName = converterMethodName(methodDescriptor)
    printer
      .add(
        s"""def $methodName(handlerRequest: HandlerRequest): $rpcMethodInputType""")
      .add("")
  }

  private def printAbstractControllerClass(
      p: FunctionalPrinter,
      serviceDescriptor: ServiceDescriptor) = {
    val controllerClassName = generateAbstractControllerName(serviceDescriptor)
    val serviceName = getServiceName(serviceDescriptor)
    val converterClassName = getConverterClassName(serviceDescriptor)
    val a = s"""
       |abstract class $controllerClassName(rpc: $serviceName, converter: $converterClassName) {
       |  ${(for (method <- serviceDescriptor.methods) yield s"""
       |  def ${controllerMethodName(method)}(handlerRequest: HandlerRequest): Future[Response]""".stripMargin).mkString}
       |}
     """.stripMargin

    p.add(a)
  }

  private def generateAbstractControllerName(m: ServiceDescriptor) =
    s"${m.getName}Controller"

  private def printAbstractControllerMethod(printer: FunctionalPrinter,
                                            m: MethodDescriptor) = {
    val methodName = controllerMethodName(m)
    printer
      .add(s"def $methodName(handlerRequest: HandlerRequest): Future[Response]")
      .add("")
  }

}
