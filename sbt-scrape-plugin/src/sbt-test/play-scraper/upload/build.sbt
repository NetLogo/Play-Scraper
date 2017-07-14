import org.nlogo.PlayScrapePlugin.credentials.{ fromCredentialsProfile, fromEnvironmentVariables }

name := """upload"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

scalaVersion := "2.12.2"

scrapeRoutes += "/other"

if (System.getenv("CREDENTIALS_FROM_ENVIRONMENT") == "true")
  scrapePublishCredential := fromEnvironmentVariables
else
  scrapePublishCredential := fromCredentialsProfile("play-scrape-tester")

scrapePublishBucketID := Some("play-scrape-test")

scrapeAbsoluteURL := Some("play-scrape-test.s3-website-us-east-1.amazonaws.com")
