# Twinagle ScalaPB Plugin

To test the generator, within SBT:

```
> scripted
```

This would publish your generator locally and run it over a test project
located under `codegen/src/sbt-test/generator/e2e`.

Assuming that the plugin If you would like to test the test project without republishing the plugin
each time (assuming it does not change), then publish it locally using
`publishLocal`, then in `codegen/src/sbt-test` run SBT and pass it the version number,
for example:

```
cd codegen/src/sbt-test/generator/e2e`
sbt -Dplugin.version=0.1.1-SNAPSHOT

> test
```
