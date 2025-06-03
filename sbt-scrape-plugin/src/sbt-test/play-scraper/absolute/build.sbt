name := """absolute"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

scalaVersion := "2.13.16"

scrapeAbsoluteURL := Some("netlogoweb.org")
