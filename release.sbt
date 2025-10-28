inThisBuild(
  List(
    // These are normal sbt settings to configure for release, skip if already defined
    organizationName     := "SoundCloud",
    description          := "An implementation of the Twirp protocol on top of Finagle",
    licenses             := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    developers           := List(
      Developer(
        id = "ccmtaylor",
        name = "Christopher Taylor",
        url = url("https://github.com/ccmtaylor")
      ),
      Developer(
        id = "rbscgh",
        name = "Rahul Bhonsale",
        url = url("https://github.com/rbscgh")
      ),
      Developer(
        id = "rafikk",
        name = "Rafik Salama",
        url = url("https://github.com/rafikk")
      ),
      Developer(
        id = "BenjaminDebeerst",
        name = "Benjamin Debeerst",
        url = url("https://github.com/BenjaminDebeerst")
      )
    ),
    scmInfo := Some(
      ScmInfo(
      )
    )
  )
)
