name := """delay"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.Scraper)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  cache
)

routesGenerator := InjectedRoutesGenerator

scrapeDelay := 5
