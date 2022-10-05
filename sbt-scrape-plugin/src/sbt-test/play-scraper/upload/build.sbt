import org.nlogo.PlayScrapePlugin.credentials.{ fromCredentialsProfile, fromEnvironmentVariables }

name := """upload"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

// Can be removed with a future Play update, see https://github.com/playframework/playframework/releases/2.8.15
// -Jeremy B October 2022
libraryDependencies ++= Seq(
  "com.google.inject"            % "guice"                % "5.1.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0"
)

scalaVersion := "2.12.17"

scrapeRoutes += "/other"

if (System.getenv("CREDENTIALS_FROM_ENVIRONMENT") == "true")
  scrapePublishCredential := fromEnvironmentVariables
else
  scrapePublishCredential := fromCredentialsProfile("play-scraper-tester")

scrapePublishBucketID := Some("play-scraper-test")

scrapeAbsoluteURL := Some("play-scraper-test.s3-website-us-east-1.amazonaws.com")
