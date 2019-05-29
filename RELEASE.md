# How to publish a release

## Prerequisites

To publish a release, two sets of credentials are necessary:
an account for `oss.sonatype.org`,
and a pgp key (and associated passphrase) for signing the artifacts.

To avoid repeted password prompts,
you can place the sonatype credentials in `~/.sbt/1.0/credentials.sbt`:

```scala
credentials += Credentials("Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        "<username>",
        "<password>"
)
```

GnuPG integrates with your system's keychain,
so you will only be asked for the passphrase once.

## Making a release

Run `sbt release` **using JDK-8**. The command will

* prompt for version numbers
* ensure that tests pass
* build and sign artifacts
* publish them to oss.sonatype.org
* release the published artifacts to maven central
