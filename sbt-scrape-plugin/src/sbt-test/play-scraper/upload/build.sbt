import org.nlogo.PlayScrapePlugin.credentials.fromCredentialsProfile

name := """upload"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

scalaVersion := "2.11.6"

scrapeRoutes += "/other"

scrapePublishCredential := fromCredentialsProfile("play-scrape-tester")

scrapePublishBucketID := Some("play-scrape-test")

scrapeAbsoluteURL := Some("play-scrape-test.s3-website-us-east-1.amazonaws.com")
