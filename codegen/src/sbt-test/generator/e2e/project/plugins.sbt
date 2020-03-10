//val pluginVersion = sys.props.get("plugin.version").getOrElse(
//        sys.error("""|The system property 'plugin.version' is not defined.
//                     |Specify this property by passing a version to SBT, for
//                     |example -Dplugin.version=0.1.1-SNAPSHOT""".stripMargin
//                 )
//)

val pluginVersion = "0.5.1-SNAPSHOT"

addSbtPlugin("com.soundcloud" % "twinagle-scalapb-plugin" % pluginVersion)
