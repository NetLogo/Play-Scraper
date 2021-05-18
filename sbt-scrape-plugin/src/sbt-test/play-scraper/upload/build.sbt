import org.nlogo.PlayScrapePlugin.credentials.{ fromCredentialsProfile, fromEnvironmentVariables }

name := """upload"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

scalaVersion := "2.12.4"

scrapeRoutes += "/other"

scrapePublishCredential := fromEnvironmentVariables

scrapePublishBucketID := Some("play-scraper-test")

scrapeAbsoluteURL := Some("play-scraper-test.s3-website-us-east-1.amazonaws.com")
