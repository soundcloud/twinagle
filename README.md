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

Please ensure that `sbt scalafmtCheckAll +test scripted` passes when submitting code changes.

# Notes

* IntelliJ doesn't run plugins during project build. Before importing,
 `sbt compile` may be necessary.

* In order to run the full test suite (i.e. the unit tests & the end-to-end tests
 for code-generation) use `sbt +test scripted`
