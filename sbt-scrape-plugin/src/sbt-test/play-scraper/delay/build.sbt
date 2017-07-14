name := """delay"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  ehcache
)

routesGenerator := InjectedRoutesGenerator

scrapeDelay := 5
