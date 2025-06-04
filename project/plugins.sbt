resolvers += "netlogo-publish-versioned" at "https://dl.cloudsmith.io/public/netlogo/publish-versioned/maven/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.7")
// Due to a weird glitch in Cloudsmith, we have to publish this with the version tog et it to go through.  It might be
// worth contacting their support if getting a regular versioned release out is important.  -Jeremy B June 2025
addSbtPlugin("org.nlogo" % "publish-versioned-plugin" % "3.0.0")
