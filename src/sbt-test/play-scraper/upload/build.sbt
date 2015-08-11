import org.nlogo.PlayScrapePlugin.credentials.fromCredentialsProfile

name := """upload"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

scalaVersion := "2.11.6"

scrapePublishCredential := fromCredentialsProfile("play-scrape-tester")

scrapePublishBucketID := Some("play-scrape-test")
