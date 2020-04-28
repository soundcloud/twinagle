# How to publish a release

Twinagle uses Github Releases.

1. Go to https://github.com/soundcloud/twinagle/releases
1. Click "Draft a new release"
1. Enter the version number for the release in the tag field
1. Update the release notes. The following template works well:
   ```
   # :warning: Breaking Changes
   
   * enrage users for good reasons (#666)
   
   # :star2: Improvements

   * embiggen the betterness (#689)

   # :beetle: Bugfixes

   * stop doing it wrong (#420)

   # :chart_with_upwards_trend: Updates

   * Finagle, probably
   * relevant version bumps for dependencies, e.g. ScalaPB
   ```
1. Publish the release when you're ready.
   A build will kick off in CI and publish the tagged release
   to Maven Central.
