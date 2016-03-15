name := """absolute"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

scalaVersion := "2.11.6"

scrapeAbsoluteURL := Some("netlogoweb.org")
