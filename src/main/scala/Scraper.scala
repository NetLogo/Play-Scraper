package org.nlogo

import play.{ Play, PlayRunHook }

import sbt.{ AutoPlugin, taskKey }

import scala.collection.JavaConversions._

object Scraper extends AutoPlugin {

  object autoImport {
    val scrapeKey = taskKey[Unit]("scrape play")
    val scrapeHooks = taskKey[Seq[PlayRunHook]]("scrape play")
  }

  import autoImport._

  override val projectSettings = Seq(
    scrapeHooks := {
      Seq(PlayRunHook.makeRunHookFromOnStarted { sock => println("I'm here!") })
    },
    scrapeKey := {
      import play.PlayImport.PlayKeys.playRunHooks
      import Play._
      Play.playRunTask(
        scrapeHooks,
        playDependencyClasspath,
        playDependencyClassLoader,
        playReloaderClasspath,
        playReloaderClassLoader,
        playAssetsClassLoader).toTask("").value
    })
}
