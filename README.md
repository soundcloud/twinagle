# Twinagle = Twirp + Finagle

Code generation and runtime support for writing Twirp services with Finagle.

[![Build Status](https://api.cirrus-ci.com/github/soundcloud/twinagle.svg)](https://cirrus-ci.com/github/soundcloud/twinagle)
# Notes

* IntelliJ [doesn't run plugins][intellij] during project build. Before importing,
 `sbt compile` may be necessary. 

# TODO

## General

* [ ] docs (expand API docs + how-to-use)
* [ ] consider using sbt-gpg instead of sbt-pgp; it seems to be better maintained and documented

## Codegen
* [ ] expose protobuf comments as scaladocs on service traits
  - [x] on the trait
  - [ ] method scaladocs
* [ ] how to handle streaming RPCs? log and error and ignore? abort?

[intellij]: https://intellij-support.jetbrains.com/hc/en-us/community/posts/206825945-sbt-tasks-as-part-of-the-normal-build
[GrpcServicePrinter]: https://github.com/scalapb/ScalaPB/blob/master/compiler-plugin/src/main/scala/scalapb/compiler/GrpcServicePrinter.scala
