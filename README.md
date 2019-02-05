# proto-plugin
Code generation for clients and services with protobuf IDL

# Notes

* IntelliJ [doesn't run plugins][intellij] during project build. Before importing,
 `sbt compile` may be necessary. 

# TODO

## General

* [ ] docs (expand API docs + how-to-use)
* should we match [Scrooge](https://twitter.github.io/scrooge/Finagle.html#creating-a-server)'s conventions?

## Codegen
* [ ] expose protobuf comments as scaladocs on service traits
  - [x] on the trait
  - [ ] method scaladocs
* [ ] build SBT plugin based on https://github.com/fiadliel/fs2-grpc
  - [ ] refactor codegen based on [`GrpcServicePrinter`][GrpcServicePrinter]. 
* [ ] make sure that multiple service definitions in proto file do something sensible
* [ ] how to handle streaming RPCs? log and error and ignore? abort?

## Runtime

* [x] use sbt-buildinfo plugin to provide correct twinagle-runtime dependency
* [ ] refactor/simplify exceptions (no separate `ErrorCode`)

[intellij]: https://intellij-support.jetbrains.com/hc/en-us/community/posts/206825945-sbt-tasks-as-part-of-the-normal-build
[GrpcServicePrinter]: https://github.com/scalapb/ScalaPB/blob/master/compiler-plugin/src/main/scala/scalapb/compiler/GrpcServicePrinter.scala