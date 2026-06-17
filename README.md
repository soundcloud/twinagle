# Twinagle = Twirp + Finagle

![Build Status](https://github.com/soundcloud/twinagle/actions/workflows/build.yml/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.soundcloud/twinagle-runtime_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.soundcloud/twinagle-runtime_2.13)

Twinagle is an implementation of the
[Twirp wire protocol](https://github.com/twitchtv/twirp/blob/master/PROTOCOL.md)
for Scala+Finagle.

Please see [the documentation website](https://soundcloud.github.io/twinagle)
for an introduction.

# How to contribute

Thanks for your interest in Twinagle, we're welcome your contributions!
For larger changes, please open an issue to discuss them before spending lots of time implementing things.
For small changes, hack away and submit a pull request.

Please ensure that `sbt scalafmtCheckAll +test +publishLocal +plugin/scripted` passes when submitting code changes.

# sbt 1.x and sbt 2.x

The `twinagle-scalapb-plugin` is cross-built for **both sbt 1.x and sbt 2.x**.
The build itself runs on an sbt 1.x launcher and cross-publishes both plugin
artifacts via `crossScalaVersions = [2.12, 3]` + `pluginCrossBuild / sbtVersion`
(Scala 2.12 → sbt 1.x, Scala 3 → sbt 2.0.0). This follows the pattern used by
sbt-protoc / sbt-assembly; see the
[sbt 2 plugin cross-building guide](https://www.scala-sbt.org/2.x/docs/en/changes/migrating-from-sbt-1.x.html#cross-building-sbt-plugins).

**Building requires JDK 17** (sbt 2.0.0 will not run on JDK 8/11).

# Notes

* IntelliJ doesn't run plugins during project build. Before importing,
 `sbt compile` may be necessary.

* In order to run the full test suite (i.e. the unit tests & the end-to-end tests
 for code-generation) use `sbt +test +publishLocal +plugin/scripted`. The
 `+plugin/scripted` runs the end-to-end test against **both** sbt 1.x and sbt 2.x;
 bare `scripted` only exercises the sbt 1.x axis.
