# Twinagle = Twirp + Finagle

![Build Status](https://github.com/soundcloud/twinagle/actions/workflows/build.yml/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.soundcloud/twinagle-runtime_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.soundcloud/twinagle-runtime_2.13)

Twinagle is an implementation of the
[Twirp wire protocol](https://github.com/twitchtv/twirp/blob/master/PROTOCOL.md)
for Scala + Finagle.

See [the documentation website](https://soundcloud.github.io/twinagle) for an
introduction.

## Contributing

Thanks for your interest in Twinagle — we welcome your contributions!

- For larger changes, please open an issue to discuss them before spending time
  on implementation.
- For small changes, hack away and submit a pull request.

Please ensure that `sbt scalafmtCheckAll +test scripted` passes before
submitting code changes.

## Notes

- IntelliJ doesn't run plugins during project build. Before importing, `sbt
  compile` may be necessary.
- To run the full test suite (unit tests plus the end-to-end code-generation
  tests), use `sbt +test scripted`.
