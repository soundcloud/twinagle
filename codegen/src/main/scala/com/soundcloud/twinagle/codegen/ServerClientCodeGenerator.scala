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
          p.add(generateServiceTrait(m)) // Service class
//            .add(generateHandlerRoutes(m)) // HandlerRouterBuilder for handling generated clients
//            .add(generateAbstractControllerClass(m)) // Trait for the Controller, which handles HTTP requests and Responses
//            .add(generateDefaultControllerClass(m)) // Controller class for handling auto-generated clients
//            .add(generateConverterTrait(m)) // Converter trait for turning HTTP objects into RPC objects
//            .add(generateDefaultConverterClass(m)) // Default converter implementation for auto-generated clients
//            .add(generateClient(m))
        }
      outputFile.setContent(fp.result)
      outputFile.build
    }

    private def generateClient(serviceDescriptor: ServiceDescriptor) = {
      val clientName = getClientName(serviceDescriptor)

      s"""
         |class $clientName(jsonClient: JsonClient) {
         |
       |${serviceDescriptor.methods.map(generateClientMethodDeclaration).mkString("\n")}
         |
       |}
     """.stripMargin

    }

    private def generateConverterTrait(serviceDescriptor: ServiceDescriptor) = {
      val converterName = getConverterClassName(serviceDescriptor)

      s"""
         |trait $converterName {
         |
       |${serviceDescriptor.methods.map(generateConverterMethodDef).mkString("\n")}
         |
       |}
     """.stripMargin
    }

    private def generateDefaultConverterClass(serviceDescriptor: ServiceDescriptor) = {
      val converterTraitName = getConverterClassName(serviceDescriptor)

      s"""
         |class Default$converterTraitName extends $converterTraitName {
         |
       |${serviceDescriptor.methods.map(generateDefaultConverterMethod).mkString("\n")}
         |
       |}
     """.stripMargin

    }

    private def generateAbstractControllerClass(serviceDescriptor: ServiceDescriptor) = {
      val controllerClassName = generateAbstractControllerName(serviceDescriptor)
      val serviceName = getServiceName(serviceDescriptor)
      val converterClassName = getConverterClassName(serviceDescriptor)
      val methods = serviceDescriptor.methods
      s"""
         |abstract class $controllerClassName(rpc: $serviceName, converter: $converterClassName) {
         |
       |${methods.map(getAbstractControllerMethod).mkString("\n")}
         |
       |}
    """.stripMargin

    }


    private def getConverterClassName(serviceDescriptor: ServiceDescriptor) = {
      s"${serviceDescriptor.getName}Converter"
    }

    private def generateDefaultControllerClass(serviceDescriptor: ServiceDescriptor) = {
      val defaultControllerName = generateDefaultControllerName(serviceDescriptor)
      val serviceName = getServiceName(serviceDescriptor)
      val converterClass = getConverterClassName(serviceDescriptor)
      val abstractControllerName = generateAbstractControllerName(serviceDescriptor)
      s"""
         |class $defaultControllerName(rpc: $serviceName, converter: $converterClass)
         |  extends $abstractControllerName(rpc, converter) {
         |
       |${serviceDescriptor.methods.map(generateDefaultControllerMethod).mkString("\n")}
         |
       |}
     """.stripMargin
    }

    private def generateHandlerRoutes(serviceDescriptor: ServiceDescriptor): String = {
      val controllerName = generateAbstractControllerName(serviceDescriptor)
      s"""
         |object ${serviceDescriptor.getName}HandlerRoutes {
         |
       |  def addRoutes(controller: $controllerName) = HandlerRouterBuilder
         |${serviceDescriptor.methods.map(generateRouteRegistrationLine).mkString("\n")}
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

    private def getClientName(serviceDescriptor: ServiceDescriptor) = s"${serviceDescriptor.getName}Client"

    private def generateHeader(fileDesc: FileDescriptor) = {
      s"""package ${fileDesc.scalaPackageName}
         |
         |import com.twitter.util.Future
         |import com.twitter.finagle.http.{Method, Response}
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
      if (str.length > 1)
        str.substring(0, 1).toLowerCase() + str.substring(1)
      else str

    private def generateClientMethodDeclaration(methodDescriptor: MethodDescriptor) = {
      val varType = methodInputType(methodDescriptor)
      val varName = decapitalize(varType)
      val outputType = methodOutputType(methodDescriptor)
      val methodName = methodDescriptor.name
      val path = generatePathForMethod(methodDescriptor)
      s"""
         |  def $methodName($varName: $varType): Future[$outputType] = {
         |    jsonClient.post(
         |      path = Path($path),
         |      body = Some(JsonFormat.toJsonString($varName))
         |    ).map { r =>
         |      JsonFormat.fromJsonString[$outputType](r.contentString)
         |    }
         |  }""".stripMargin
    }

    private def generatePathForMethod(methodDescriptor: MethodDescriptor) = {
      s""""/rpc/${methodDescriptor.getService.getFullName}/${methodDescriptor.name.capitalize}""""
    }

    private def generateRouteRegistrationLine(
                                               methodDescriptor: MethodDescriptor): String = {
      val path = generatePathForMethod(methodDescriptor)
      s"""    .register(Method.Post, $path, controller.${controllerMethodName(methodDescriptor)})"""
    }

    private def generateDefaultControllerMethod(methodDescriptor: MethodDescriptor) = {
      val controllerMethod = controllerMethodName(methodDescriptor)
      val rpcMethodName = methodDescriptor.name
      s"""
         |  def $controllerMethod(handlerRequest: HandlerRequest): Future[Response] = {
         |    val req = converter.${converterMethodName(methodDescriptor)}(handlerRequest)
         |
       |    rpc.$rpcMethodName(req)
         |      .map(JsonFormat.toJsonString)
         |      .map(JsonResponseBuilder.ok)
         |  }""".stripMargin
    }

    private def generateDefaultControllerName(m: ServiceDescriptor) =
      s"Default${m.getName}Controller"

    private def converterMethodName(m: MethodDescriptor) =
      s"convertTo${methodInputType(m)}"

    private def generateDefaultConverterMethod(methodDescriptor: MethodDescriptor) = {
      val rpcMethodInputType = methodInputType(methodDescriptor)
      val methodName = converterMethodName(methodDescriptor)

      s"""
         |def $methodName(handlerRequest: HandlerRequest): $rpcMethodInputType = {
         |  JsonFormat.fromJsonString[$rpcMethodInputType](handlerRequest.contentString)
         |}""".stripMargin

    }

    private def generateConverterMethodDef(methodDescriptor: MethodDescriptor) = {
      val rpcMethodInputType = methodInputType(methodDescriptor)
      val methodName = converterMethodName(methodDescriptor)
      s"""|  def $methodName(handlerRequest: HandlerRequest): $rpcMethodInputType""".stripMargin
    }

    private def getAbstractControllerMethod(method: MethodDescriptor) =
      s"""|  def ${controllerMethodName(method)}(handlerRequest: HandlerRequest): Future[Response]""".stripMargin

    private def generateAbstractControllerName(m: ServiceDescriptor) =
      s"${m.getName}Controller"


    private def controllerMethodName(m: MethodDescriptor) =
      s"handle${m.name.capitalize}"

  }

}

