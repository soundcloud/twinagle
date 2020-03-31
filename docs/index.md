---
layout: page
title: Twinagle
---

## About

Twinagle is an implementation of the
[Twirp wire protocol](https://github.com/twitchtv/twirp/blob/master/PROTOCOL.md)
for Scala+Finagle.

It allows developers who work in Scala/Finagle to create microservices
which have [protobuf IDL](https://developers.google.com/protocol-buffers/docs/proto3)-described
interfaces and to (optionally) send and receive [binary protobuf](https://developers.google.com/protocol-buffers/docs/encoding)
over http.

## Key References

* [SoundCloud Backstage blog post]() about the launch of this project
* [Blog post launching Twirp](https://blog.twitch.tv/en/2018/01/16/twirp-a-sweet-new-rpc-framework-for-go-5f2febbf35f/)
* [Twirp golang implementation](https://github.com/twitchtv/twirp)
* [Twirp wire protocol](https://github.com/twitchtv/twirp/blob/master/PROTOCOL.md)
* [Protocol Buffers: Intro](https://developers.google.com/protocol-buffers/docs/overview)
* [Finagle: Intro](https://blog.twitter.com/engineering/en_us/a/2011/finagle-a-protocol-agnostic-rpc-system.html)

## Project setup

To get started with twinagle, you'll need to add a plugin dependency to your project and enable the plugin for your build.
Add the folowing line to `project/plugins.sbt`:

```scala
addSbtPlugin("com.soundcloud" % "twinagle-scalapb-plugin" % <version>)
```

Then, enable the plugin in your build by adding this to `build.sbt`:

```scala
enablePlugins(Twinagle)
```

## Defining the API

Twirp APIs are defined in Protobuf files.
By convention, Twinagle expects them to be under `src/main/protobuf`.

For example, place this under `src/main/protobuf/haberdasher.proto`:

```proto
syntax = "proto3";

package twitch.twirp.example.haberdasher;

// A Hat is a piece of headwear made by a Haberdasher.
message Hat {
  // The size of a hat should always be in inches.
  int32 size = 1;

  // The color of a hat will never be 'invisible', but other than
  // that, anything is fair game.
  string color = 2;

  // The name of a hat is it's type. Like, 'bowler', or something.
  string name = 3;
}

// Size is passed when requesting a new hat to be made. It's always measured in
// inches.
message Size {
  int32 inches = 1;
}

// A Haberdasher makes hats for clients.
service Haberdasher {
  // MakeHat produces a hat of mysterious, randomly-selected color!
  rpc MakeHat(Size) returns (Hat);
}
```

## Generating Code

When you compile your project in SBT (e.g. via `sbt compile` or `sbt test`),
Twinagle will generate code from the API definition.
