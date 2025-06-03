resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.github.sbt" % "sbt-digest" % "2.1.0")

def getPluginVersion(key: String) = {
  val version = System.getProperty(key)
  if (version == null) {
    throw new RuntimeException(s"""|The system property '$key' is not defined.
      |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  }
  version
}

{
  addSbtPlugin("com.typesafe.play" % "sbt-plugin" % getPluginVersion("play.version"))
  addSbtPlugin("org.nlogo" %% "play-scraper" % getPluginVersion("plugin.version"))
}
