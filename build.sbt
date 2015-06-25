sbtPlugin := true

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

name := "play-scraper"

organization := "org.nlogo"

version := "0.2"

isSnapshot := true

licenses += ("Public Domain", url("http://creativecommons.org/licenses/publicdomain/"))

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-feature", "-deprecation")

addSbtPlugin(("com.typesafe.play" % "sbt-plugin" % "2.4.0").extra("scalaVersion" -> "2.10"))

libraryDependencies += "com.typesafe.play" %% "play" % "2.4.0"
