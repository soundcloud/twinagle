package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors.{MethodDescriptor, ServiceDescriptor}
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter}

final class TwinagleServicePrinter(
    service: ServiceDescriptor,
    implicits: DescriptorImplicits
) {
  import implicits._

  private[this] val twitterUtil = "_root_.com.twitter.util"
  private[this] val finagle     = "_root_.com.twitter.finagle"
  private[this] val finagleHttp = s"$finagle.http"
  private[this] val twinagle    = "_root_.com.soundcloud.twinagle"

  private[this] val Future   = s"$twitterUtil.Future"
  private[this] val Service  = s"$finagle.Service"
  private[this] val Filter   = s"$finagle.Filter"
  private[this] val Request  = s"$finagleHttp.Request"
  private[this] val Response = s"$finagleHttp.Response"

  private[this] val EndpointMetadata      = s"$twinagle.EndpointMetadata"
  private[this] val ClientEndpointBuilder = s"$twinagle.ClientEndpointBuilder"
  private[this] val ServerBuilder         = s"$twinagle.ServerBuilder"
  private[this] val ProtoService          = s"$twinagle.ProtoService"
  private[this] val AsProtoService        = s"$twinagle.AsProtoService"
  private[this] val ProtoRpc              = s"$twinagle.ProtoRpc"

  def generateServiceObject(m: ServiceDescriptor): String = {
    val serviceName = getServiceName(m)
    s"""
       |object $serviceName {
       |  implicit val asProtoService: $AsProtoService[$serviceName] = (service: $serviceName) => $ProtoService(Seq(
       |${m.methods.map(protoRpc).mkString(",\n")}
       |  ))
       |
       |  def server(service: $serviceName,
       |             extension: $EndpointMetadata => $Filter.TypeAgnostic = _ => $Filter.TypeAgnostic.Identity,
       |             prefix: String = "/twirp"
       |             ): $Service[$Request, $Response] =
       |    $ServerBuilder(extension).withPrefix(prefix).register(service).build
       |}
       """.stripMargin
  }

  def protoRpc(md: MethodDescriptor): String = {
    val meta = endpointMetadata(md)
    val rpc  = s"service.${decapitalizedName(md)}"
    s"    $ProtoRpc($meta, $rpc _)"
  }

  def endpointMetadata(md: MethodDescriptor): String = {
    val svc        = md.getService.getFullName
    val methodName = getName(md)
    s"""$EndpointMetadata("$svc", "$methodName")"""
  }

  def printService(printer: FunctionalPrinter): FunctionalPrinter = {
    printer
      .add(generateHeader(service.getFile.scalaPackage.fullName))
      .add(generateServiceTrait(service))
      .add(generateServiceObject(service))
      .add(generateProtobufClient(service))
      .add(generateJsonClient(service))
  }

  private def generateJsonClient(serviceDescriptor: ServiceDescriptor) = {
    val clientName  = getClientName(serviceDescriptor)
    val serviceName = getServiceName(serviceDescriptor)

    val jsonClients = serviceDescriptor.methods
      .map(generateJsonClientService)
      .mkString("\n")
    val methods = serviceDescriptor.methods
      .map(generateGenericClientMethod)
      .mkString("\n")

    s"""
       |class ${clientName}Json(httpClient: $Service[$Request, $Response],
       |                        extension: $EndpointMetadata => $Filter.TypeAgnostic = _ => $Filter.TypeAgnostic.Identity,
       |                        prefix: String = "/twirp")
       |  extends $serviceName {
       |
       |  private val _builder = new $ClientEndpointBuilder(httpClient, extension, prefix)
       |
       |$jsonClients
       |$methods
       |}
       """.stripMargin
  }

  private def generateProtobufClient(serviceDescriptor: ServiceDescriptor) = {
    val clientName  = getClientName(serviceDescriptor)
    val serviceName = getServiceName(serviceDescriptor)

    val protobufClients = serviceDescriptor.methods
      .map(generateProtobufClientService)
      .mkString("\n")
    val methods = serviceDescriptor.methods
      .map(generateGenericClientMethod)
      .mkString("\n")

    s"""
       |class ${clientName}Protobuf(httpClient: $Service[$Request, $Response],
       |                            extension: $EndpointMetadata => $Filter.TypeAgnostic = _ => $Filter.TypeAgnostic.Identity,
       |                            prefix: String = "/twirp")
       |  extends $serviceName {
       |
       |  private val _builder = new $ClientEndpointBuilder(httpClient, extension, prefix)
       |
       |$protobufClients
       |$methods
       |}
       """.stripMargin
  }

  private def generateServiceTrait(
      serviceDescriptor: ServiceDescriptor
  ): String = {
    val comment           = serviceDescriptor.comment
    val docString: String = commentToDocString(comment)
    val serviceName       = getServiceName(serviceDescriptor)
    val methods           = serviceDescriptor.methods
    s"""
       |$docString
       |trait $serviceName {
       |${methods.map(getServiceMethodDeclaration).mkString("\n")}
       |}
     """.stripMargin
  }

  private def getServiceName(serviceDescriptor: ServiceDescriptor) =
    s"${serviceDescriptor.getName}Service"

  private def getClientName(serviceDescriptor: ServiceDescriptor) =
    s"${serviceDescriptor.getName}Client"

  private def generateHeader(packageName: String) = s"package $packageName"

  private def getServiceMethodDeclaration(
      methodDescriptor: MethodDescriptor
  ) = {
    val varType    = methodInputType(methodDescriptor)
    val varName    = decapitalize(varType)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = decapitalizedName(methodDescriptor)
    val docString = commentToDocString(methodDescriptor.comment).replace(
      "\n",
      "\n  "
    ) // First line is indented by "|  " and the next lines are indented with the replace
    s"""
       |  $docString
       |  def $methodName($varName: $varType): $Future[$outputType]""".stripMargin
  }

  private def methodInputType(methodDescriptor: MethodDescriptor) = {
    lastPart(methodDescriptor.inputType.scalaType)
  }

  private def methodOutputType(methodDescriptor: MethodDescriptor) = {
    lastPart(methodDescriptor.outputType.scalaType)
  }

  private def lastPart(str: String): String =
    str.lastIndexOf(".") match {
      case i if i > 0 => str.substring(i + 1)
      case _          => str
    }

  private def decapitalize(str: String) =
    if (str.length >= 1)
      str.substring(0, 1).toLowerCase() + str.substring(1)
    else str

  private def generateJsonClientService(
      methodDescriptor: MethodDescriptor
  ): String = {
    val inputType  = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = decapitalizedName(methodDescriptor)
    val endpoint   = endpointMetadata(methodDescriptor)

    s"""
       |  private val _${methodName}Service: $Service[$inputType, $outputType] = {
       |    implicit val companion = $outputType
       |    _builder.jsonEndpoint($endpoint)
       |  }
      """.stripMargin
  }

  private def generateProtobufClientService(
      methodDescriptor: MethodDescriptor
  ): String = {
    val inputType  = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = decapitalizedName(methodDescriptor)
    val endpoint   = endpointMetadata(methodDescriptor)

    s"""
       |  private val _${methodName}Service: $Service[$inputType, $outputType] = {
       |    implicit val companion = $outputType
       |    _builder.protoEndpoint($endpoint)
       |  }
      """.stripMargin
  }

  private def generateGenericClientMethod(
      methodDescriptor: MethodDescriptor
  ) = {
    val inputType  = methodInputType(methodDescriptor)
    val outputType = methodOutputType(methodDescriptor)
    val methodName = decapitalizedName(methodDescriptor)
    s"""
       |  override def $methodName(request: $inputType): $Future[$outputType] = _${methodName}Service(request)
       """.stripMargin
  }

  private def decapitalizedName(m: MethodDescriptor) = {
    m.name // Name from the protobuf, but will lowercase the first letter
  }

  private def getName(m: MethodDescriptor) = {
    m.getName // Name exactly as it appears in the protobuf
  }

  private def commentToDocString(comment: Option[String]) = {
    val docLines: Seq[String] =
      comment.map(_.split('\n').toSeq).getOrElse(Seq.empty)
    if (docLines.nonEmpty) {
      s"""/**
         |${docLines.map("  * " + _).mkString("\n")}
         |  */""".stripMargin
    } else {
      ""
    }
  }
}
