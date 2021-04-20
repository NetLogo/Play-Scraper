lazy val playVersion = "2.6.12"

lazy val sharedSettings = Seq(
    organization := "org.nlogo"
  , version      := "0.8.1"
  , isSnapshot   := true
  , licenses     += ("Creative Commons Zero v1.0 Universal Public Domain Dedication", url("https://creativecommons.org/publicdomain/zero/1.0/"))
  , publishTo    := { Some("Cloudsmith API" at "https://maven.cloudsmith.io/netlogo/play-scraper/") }
)

lazy val root = project.in(file(".")).
  aggregate(playScrape, playScrapeServer).
  settings(sharedSettings).
  settings(
    publishLocal := {
      (publishLocal in playScrape).value
      (publishLocal in playScrapeServer).value
    }
  )

lazy val playScrape = project.in(file("sbt-scrape-plugin")).
  settings(sharedSettings).
  settings(scriptedSettings).
  settings(
    sbtPlugin           := true,
    name                := "play-scraper",
    scalaVersion        := "2.12.4",
    scalacOptions       ++= Seq("-feature", "-deprecation"),
    addSbtPlugin(("com.typesafe.play" % "sbt-plugin" % playVersion).extra("scalaVersion" -> "2.12")),
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-cloudfront" % "1.11.291",
      "com.amazonaws" % "aws-java-sdk-s3"         % "1.11.291",
      "commons-codec" % "commons-codec"           % "1.10"
    ))

lazy val playScrapeServer = project.in(file("play-scrape-server")).
  settings(sharedSettings).
  settings(
    name                := "play-scrape-server",
    scalaVersion        := "2.12.4",
    scalacOptions       ++= Seq("-deprecation"),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % playVersion,
      guice
    ))

lazy val scriptedSettings =
  Seq(
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false)
