name := """delay"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

scalaVersion := "3.7.0"

libraryDependencies ++= Seq(ehcache)

routesGenerator := InjectedRoutesGenerator

scrapeDelay := 5
