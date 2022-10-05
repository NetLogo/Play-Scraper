resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

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
