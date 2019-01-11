# Twinagle ScalaPB Plugin

To test the generator, within SBT:

```
> scripted
```

This would publish your generator locally and run it over a test project
located under `src/sbt-test/generator/e2e`.

Assuming that the plugin If you would like to test the test project without republishing the plugin
each time (assuming it does not change), then publish it locally using
`publishLocal`, then in `src/sbt-test` run SBT and pass it the version number,
for example:

```
cd src/sbt-test/generator/e2e`
sbt -Dplugin.version=0.1.0-SNAPSHOT

> test
```
