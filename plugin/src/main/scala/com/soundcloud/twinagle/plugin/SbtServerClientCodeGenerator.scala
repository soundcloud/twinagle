
import protocbridge.Artifact

object SbtServerClientCodeGenerator extends ServerClientCodeGenerator {

  override def suggestedDependencies: Seq[Artifact] = Seq(
    Artifact(
      "twinagle-runtime",
      BuildInfo.version,
      crossVersion = true
    ),
    Artifact(
      "com.thesamet.scalapb",
      "scalapb-runtime",
      scalapb.compiler.Version.scalapbVersion,
      crossVersion = true
    )
  )

}
