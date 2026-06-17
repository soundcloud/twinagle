# Twinagle ScalaPB Plugin

This plugin is cross-built for **sbt 1.x and sbt 2.x**. The end-to-end test under
`plugin/src/sbt-test/generator/e2e` runs against both: on the Scala 2.12 axis it
launches sbt 1.x, and on the Scala 3 axis it launches sbt 2.0.0.

## Running the generator tests

To test the generator across both sbt axes, within sbt (requires JDK 17):

```
> +publishLocal
> +plugin/scripted
```

`+publishLocal` cross-publishes `twinagle-runtime`, `twinagle-codegen`, and both
plugin artifacts (`_2.12_1.0` and `_sbt2_3`) to your local ivy repo. `+plugin/scripted`
then publishes nothing further and runs the test project once per sbt axis.

> Note: a bare `scripted` only runs the sbt 1.x axis. Use `+plugin/scripted` to
> cover sbt 2.x as well.

## Iterating on the test project without republishing

If the plugin itself hasn't changed, publish it once and run the test project
directly, passing the version number:

```
sbt +publishLocal     # note the version it prints, e.g. 1.4.6-SNAPSHOT

cd plugin/src/sbt-test/generator/e2e
sbt -Dplugin.version=1.4.6-SNAPSHOT
> test
```
