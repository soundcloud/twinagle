package com.soundcloud.twinagle.codegen

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.{Descriptor, FileDescriptor, MethodDescriptor, ServiceDescriptor}
import scalapb.compiler.DescriptorImplicits

import scala.collection.JavaConverters._

final class TwinagleServicePrinter(
    fileDescriptor: FileDescriptor,
    implicits: DescriptorImplicits
) {

  import implicits._

  def generate(): String = {
    val paths       = fileDescriptor.getServices.asScala.map(generatePaths).mkString("\n")
    val definitions = fileDescriptor.getMessageTypes.asScala.map(generateDefinition(_))
    s"""
       |swagger: "2.0"
       |host: "0.0.0.0:5000"
       |basePath: "/twirp"
       |schemes:
       |- "http"
       |paths:
       |$paths
       |definitions:
       |$definitions
     """.stripMargin
  }

  def generateDefinition(descriptors: Seq[Descriptor]) = {
    if (descriptors.isEmpty) {
      //Base case
    } else {
      descriptors.head.getFields
      generateDefinition(fields.tail)
    }
  }

  def generatePaths(service: ServiceDescriptor): String = {
    val paths = service.getMethods.asScala.map(generatePath).mkString("\n")
    s"""
      |$paths
    """.stripMargin
  }

  def generatePath(method: MethodDescriptor): String = {
    s"""
       |  /${method.getService.getFullName}/${getMethodName(method)}:
       |    post:
       |      operationId: "${getMethodName(method)}"
       |      consumes:
       |      - "application/json"
       |      - "application/protobuf"
       |      produces:
       |      - "application/json"
       |      - "application/protobuf"
       |      parameters:
       |      - in: "body"
       |        name: "${methodInputType(method)}"
       |        description: ""
       |        required: true
       |        schema:
       |          $$ref: "#/definitions/${methodInputType(method)}"
       |      responses:
       |        "200":
       |          description: "successful operation"
       |          schema:
       |            $$ref: "#/definitions/${methodOutputType(method)}"
     """.stripMargin
  }

  private def getMethodName(method: MethodDescriptor) = {
    method.getName
  }

  private def methodOutputType(method: MethodDescriptor) = {
    method.getOutputType.getName
  }

  private def methodInputType(method: MethodDescriptor) = {
    method.getInputType.getName
  }
}
