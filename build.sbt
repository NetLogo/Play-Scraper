lazy val playVersion = "2.9.7"

lazy val sharedSettings = Seq(
  organization := "org.nlogo"
, version      := "1.1.0"
, isSnapshot   := true
, licenses     += ("Creative Commons Zero v1.0 Universal Public Domain Dedication", url("https://creativecommons.org/publicdomain/zero/1.0/"))
, publishTo    := { Some("Cloudsmith API" at "https://maven.cloudsmith.io/netlogo/play-scraper/") }
, scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:_",
  "-Ywarn-value-discard",
  "-Xfatal-warnings"
)
)

lazy val root = project.in(file(".")).
  aggregate(playScrape, playScrapeServer).
  settings(sharedSettings).
  settings(
    publishLocal := {
      (playScrape / publishLocal).value
      (playScrapeServer / publishLocal).value
    }
  )

lazy val playScrape = project.in(file("sbt-scrape-plugin")).
  enablePlugins(SbtPlugin).
  settings(sharedSettings).
  settings(
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq(
        s"-Dplugin.version=${version.value}"
      , s"-Dplay.version=$playVersion"
      )
    }
  , scriptedBufferLog := false
  , sbtPlugin         := true
  , name              := "play-scraper"
  // sbt 1.11 only supports Scala 2.12 at the moment, so this can't yet change.  -Jeremy B June 2025
  , scalaVersion      := "2.12.17"
  , addSbtPlugin(("com.typesafe.play" % "sbt-plugin" % playVersion).extra("scalaVersion" -> "2.13"))
  , libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-cloudfront" % "1.12.317",
      "com.amazonaws" % "aws-java-sdk-s3"         % "1.12.317",
      "commons-codec" % "commons-codec"           % "1.10"
    )
  , evictionErrorLevel := Level.Warn
  )

lazy val playScrapeServer = project.in(file("play-scrape-server")).
  settings(sharedSettings).
  settings(
    name                := "play-scrape-server",
    scalaVersion        := "2.13.16",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % playVersion,
      guice
    ),
    evictionErrorLevel := Level.Warn
  )
