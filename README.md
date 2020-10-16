# Twinagle = Twirp + Finagle

Twinagle is an implementation of the
[Twirp wire protocol](https://github.com/twitchtv/twirp/blob/master/PROTOCOL.md)
for Scala+Finagle.

Please see [the documentation website](https://soundcloud.github.io/twinagle)
for an introduction.

# Contribute

Ensure `sbt +test scripted` passes before pushing.

# Build and Release

![Build Status](https://github.com/soundcloud/twinagle/workflows/Scala%20CI/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.soundcloud/twinagle-runtime_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.soundcloud/twinagle-runtime_2.12)

# Notes

* IntelliJ [doesn't run plugins][intellij] during project build. Before importing,
 `sbt compile` may be necessary.
