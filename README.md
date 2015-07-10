# Play-Scraper

An SBT plugin to enable play sites to be served statically.

## How Do I Use It?
To use, add the following to your `plugins.sbt`
```scala
resolvers += Resolver.url(
  "play-scraper",
    url("http://dl.bintray.com/content/netlogo/play-scraper"))(
        Resolver.ivyStylePatterns)

addSbtPlugin("org.nlogo" % "play-scraper" % "0.5")
```

In your `build.sbt`, add:
```scala
enablePlugins(org.nlogo.PlayScrapePlugin)
scrapeRoutes ++= Seq("/foo", "/bar.js")
```

You will then have access to the `scrapePlay` sbt task, which will scrape the routes specified, plus play assets, and put them in `target/play-scrape`.

## How Do I Build It?

Use sbt. Test it using the `scripted` task.

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The Play Scraper plugin is in the public domain.  To the extent possible under law, Uri Wilensky has waived all copyright and related or neighboring rights.
