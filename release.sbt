inThisBuild(
  List(
    // These are normal sbt settings to configure for release, skip if already defined
    organization         := "com.soundcloud",
    organizationName     := "SoundCloud",
    organizationHomepage := Some(url("https://developers.soundcloud.com/")),
    description          := "An implementation of the Twirp protocol on top of Finagle",
    licenses             := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage             := Some(url("https://github.com/soundcloud/twinagle")),
    developers := List(
      Developer(
        id = "ccmtaylor",
        name = "Christopher Taylor",
        email = "christopher.taylor@soundcloud.com",
        url = url("https://github.com/ccmtaylor")
      ),
      Developer(
        id = "oberkowitz",
        name = "Oren Berkowitz",
        email = "oren.berkowitz@soundcloud.com",
        url = url("https://github.com/oberkowitz")
      ),
      Developer(
        id = "rbscgh",
        name = "Rahul Bhonsale",
        email = "rahul@soundcloud.com",
        url = url("https://github.com/rbscgh")
      )
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/soundcloud/twinagle"),
        "scm:git@github.com:soundcloud/twinagle.git"
      )
    )
  )
)
