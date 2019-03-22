package com.soundcloud.twinagle.codegen

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.{MethodDescriptor, ServiceDescriptor}
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter, Helper}

final class TwinagleServicePrinter(service: ServiceDescriptor, implicits: DescriptorImplicits) {

  import implicits._

  private[this] val twitterUtil = "_root_.com.twitter.util"
  private[this] val finagle = "_root_.com.twitter.finagle"
  private[this] val finagleHttp = s"$finagle.http"
  private[this] val twinagle = "_root_.com.soundcloud.twinagle"

  private[this] val Future = s"$twitterUtil.Future"
  private[this] val Service = s"$finagle.Service"
  private[this] val Filter = s"$finagle.Filter"
  private[this] val ServiceFactory = s"$finagle.ServiceFactory"
  private[this] val Request = s"$finagleHttp.Request"
  private[this] val Response = s"$finagleHttp.Response"

  private[this] val Server = s"$twinagle.Server"
  private[this] val ServiceAdapter = s"$twinagle.ServiceAdapter"
  private[this] val EndpointMetadata = s"$twinagle.EndpointMetadata"

  private[this] val Http = s"$finagle.Http"
  private[this] val InetSocketAddress = s"_root_.java.net.InetSocketAddress"


  def generateServiceObject(m: ServiceDescriptor): String = {
    val serviceName = getServiceName(m)
    s"""
       |object $serviceName {
       |  def server(service: $serviceName,
       |             extension: $EndpointMetadata => $Filter.TypeAgnostic = _ => $Filter.TypeAgnostic.Identity): $Service[$Request, $Response] =
       |    new $Server(Map(
       |${m.methods.map(generateEndpoint).mkString(",\n")}
       |  ))
       |}
       """.stripMargin
  }

  def generateEndpoint(md: MethodDescriptor): String = {
    val prefix = "/twirp"
    val svc = md.getService.getFullName
    val methodName = decapitalizedName(md)
    val inputType = methodInputType(md)
    val outputType = methodOutputType(md)

    s"""      {
       |        val endpoint = $EndpointMetadata("$prefix", "$svc", "$methodName")
       |        val httpService: $Service[$Request, $Response] =
       |          new $ServiceAdapter(service.${decapitalizedName(md)})
       |        endpoint -> extension(endpoint).toFilter.andThen(httpService)
       |      }
     """.stripMargin
  }

  def printService(printer: FunctionalPrinter): FunctionalPrinter = {

    printer
      .add(generateHeader(service.getFile.scalaPackageName))
      .add(generateServiceTrait(service))
      .add(generateServiceObject(service))
      .add(generateJsonClient(service))
      .add(generateProtobufClient(service))
      .add(generateServer(service))
  }

  private def generateJsonClient(serviceDescriptor: ServiceDescriptor) = {
    val clientName = getClientName(serviceDescriptor)
    val serviceName = getServiceName(serviceDescriptor)

    s"""
       |class ${clientName}Json(httpClient: $Service[$Request, $Response],
       |                        extension: $EndpointMetadata => $Filter.TypeAgnostic = _ => $Filter.TypeAgnostic.Identity)
       |  extends $serviceName {
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
       |class ${clientName}Protobuf(httpClient: $Service[$Request, $Response],
       |                            extension: $EndpointMetadata => $Filter.TypeAgnostic = _ => $Filter.TypeAgnostic.Identity)
       |  extends $serviceName {
       |
       |${serviceDescriptor.methods.map(generateProtobufClientService).mkString("\n")}
       |${serviceDescriptor.methods.map(generateGenericClientMethod).mkString("\n")}
       |}
       """.stripMargin

  }

  implicit final class ServiceExt(message: ServiceDescriptor) {

    def sourcePath: Seq[Int] = Seq(FileDescriptorProto.SERVICE_FIELD_NUMBER, message.getIndex)

    def comment: Option[String] = {
      message.getFile
        .findLocationByPath(sourcePath)
        .map(t => t.getLeadingComments + t.getTrailingComments)
        .map(Helper.escapeComment)
        .filter(_.nonEmpty)
    }
  }

  private def generateServiceTrait(serviceDescriptor: ServiceDescriptor): String = {
    val docLines: Seq[String] = serviceDescriptor.comment.map(_.split('\n').toSeq).getOrElse(Seq.empty)
    val docString = if (docLines.nonEmpty) {
      s"""
         |/**
         |${docLines.map("  * " + _).mkString("\n")}
         |  */
         """.stripMargin
    } else {
      ""
    }
    val serviceName = getServiceName(serviceDescriptor)
    val methods = serviceDescriptor.methods
    s"""
       |${docString}trait $serviceName {
       |
       |${methods.map(getServiceMethodDeclaration).mkString("\n")}
       |}
     """.stripMargin
  }

  private def getServiceName(serviceDescriptor: ServiceDescriptor) = s"${serviceDescriptor.getName}Service"

  private def getServerName(serviceDescriptor: ServiceDescriptor) = s"${serviceDescriptor.getName}Server"

  private def getClientName(serviceDescriptor: ServiceDescriptor) = s"${serviceDescriptor.getName}Client"

  private def generateHeader(packageName: String) = s"package $packageName"

  private def getServiceMethodDeclaration(methodDescriptor: MethodDescriptor) = {
    val varType = methodInputType(methodDescriptor)
    val varName = decapitalize(varType)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = decapitalizedName(methodDescriptor)
    s"  def $methodName($varName: $varType): $Future[$outputType]"
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
    val inputType = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = decapitalizedName(methodDescriptor)
    val path = generatePathForMethod(methodDescriptor)
    val (serviceName, rpcName) = (methodDescriptor.getService.getFullName, getName(methodDescriptor))
    val endpoint = s"""$EndpointMetadata("/twirp", "$serviceName", "$rpcName")"""
    val clientFilterName = """JsonClientFilter"""
    s"""
       |  private val ${methodName}Service: $Service[$inputType, $outputType] = {
       |    implicit val companion = $outputType
       |    extension($endpoint).toFilter andThen
       |      new $twinagle.$clientFilterName[$inputType, $outputType]($path) andThen
       |      new $twinagle.TwirpHttpClient andThen
       |      httpClient
       |  }
      """.stripMargin
  }

  private def generateProtobufClientService(methodDescriptor: MethodDescriptor): String = {
    val inputType = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = decapitalizedName(methodDescriptor)
    val path = generatePathForMethod(methodDescriptor)
    val (serviceName, rpcName) = (methodDescriptor.getService.getFullName, getName(methodDescriptor))
    val endpoint = s"""$EndpointMetadata("/twirp", "$serviceName", "$rpcName")"""
    s"""
       |  private val ${methodName}Service: $Service[$inputType, $outputType] = {
       |    implicit val companion = $outputType
       |    extension($endpoint).toFilter andThen
       |      new $twinagle.ProtobufClientFilter[$inputType, $outputType]($path) andThen
       |      new $twinagle.TwirpHttpClient andThen
       |      httpClient
       |  }
      """.stripMargin
  }

  private def generateGenericClientMethod(methodDescriptor: MethodDescriptor) = {
    val inputType = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = decapitalizedName(methodDescriptor)
    s"""
       |  override def $methodName(request: $inputType): $Future[$outputType] = ${methodName}Service(request)
       """.stripMargin
  }

  private def generatePathForMethod(methodDescriptor: MethodDescriptor) = {
    s""""/twirp/${methodDescriptor.getService.getFullName}/${decapitalizedName(methodDescriptor)}""""
  }

  private def generateServer(serviceDescriptor: ServiceDescriptor) = {
    val serviceName = getServiceName(serviceDescriptor)
    val serverName = getServerName(serviceDescriptor)
    s"""
       |class $serverName(service: $serviceName, port: Int) {
       |  def build = {
       |    val server = $serviceName.server(service)
       |    $Http.server.serve(new $InetSocketAddress(port), $ServiceFactory.const(server))
       |  }
       |}
       |
       """.stripMargin
  }

  private def decapitalizedName(m: MethodDescriptor) = {
    m.name   // Name from the protobuf, but will lowercase the first letter
  }

  private def getName(m: MethodDescriptor) = {
    m.getName   // Name exactly as it appears in the protobuf
  }

}
