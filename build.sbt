lazy val playVersion = "2.5.0"

lazy val sharedSettings = Seq(
  organization := "org.nlogo",
  version      := "0.7.1",
  isSnapshot   := true,
  resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  licenses  += ("Public Domain", url("http://creativecommons.org/licenses/publicdomain/")),
  bintrayRepository   := "play-scraper",
  bintrayOrganization := Some("netlogo")
)

lazy val root = project.in(file(".")).
  aggregate(playScrape, playScrapeServer).
  settings(sharedSettings).
  settings(scriptedSettings).
  settings(
    publishLocal := {
      (publishLocal in playScrape).value
      (publishLocal in playScrapeServer).value
    }
  )

lazy val playScrape = project.in(file("sbt-scrape-plugin")).
  settings(sharedSettings).
  settings(
    sbtPlugin           := true,
    name                := "play-scraper",
    scalaVersion        := "2.10.6",
    scalacOptions       ++= Seq("-feature", "-deprecation"),
    addSbtPlugin(("com.typesafe.play" % "sbt-plugin" % playVersion).extra("scalaVersion" -> "2.10")),
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-cloudfront" % "1.10.59",
      "com.amazonaws" % "aws-java-sdk-s3"         % "1.10.59",
      "commons-codec" % "commons-codec"           % "1.10"
    ))

lazy val playScrapeServer = project.in(file("play-scrape-server")).
  settings(sharedSettings).
  settings(
    name                := "play-scrape-server",
    scalaVersion        := "2.11.7",
    libraryDependencies += "com.typesafe.play" %% "play" % playVersion)


lazy val scriptedSettings =
  ScriptedPlugin.scriptedSettings ++
  Seq(
    sbtTestDirectory := baseDirectory.value / "sbt-scrape-plugin" / "src" / "sbt-test",
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false)
