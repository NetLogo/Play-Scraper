# Play-Scraper

An SBT plugin to enable Play sites to be served statically.

## How Do I Use It?

To use, add the following to your `plugins.sbt`

```scala
resolvers += "play-scraper" at "https://dl.cloudsmith.io/public/netlogo/play-scraper/maven/"

addSbtPlugin("org.nlogo" % "play-scraper" % "0.8.1")
```

In your `build.sbt`, add:
```scala
enablePlugins(org.nlogo.PlayScrapePlugin)
scrapeRoutes ++= Seq("/foo", "/bar.js")
```

You will then have access to the `scrapePlay` sbt task, which will scrape the routes specified, plus play assets, and put them in `target/play-scrape`.
Note that version 0.7.0 is compatible only with Play 2.5. Versions of play earlier than 2.5 should use 0.6.2.

### Uploading

To enable uploading, set the following keys:

```scala
scrapePublishBucketID := Some("your-bucket")

scrapePublishRegion := Some("us-east-1")

// this is optional. When set, the distribution will be invalidated on upload
scrapePublishDistributionID := Some("your-cloudfront-distribution-id")
```

As well as settings `scrapePublishCredentials` via one of the following methods:

* profile credentials ([more info](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#config-settings-and-precedence)), use the following:
```scala
scrapePublishCredentials := org.nlogo.PlayScrapePlugin.credentials.fromCredentialsProfile("your credential name")
```

* environment variables. Note that this expects `AWS_ACCESS_KEY_ID`/`AWS_ACCESS_KEY` and `AWS_SECRET_KEY`/`AWS_SECRET_ACCESS_KEY` to be set in your environment.
```scala
scrapePublishCredentials := org.nlogo.PlayScrapePlugin.credentials.fromEnvironmentVariables
```

Once these values are set, running `scrapeUpload` will upload the newly-created play scrape.
Note that if `scrapePublishBucketID` isn't set, the task will print a warning but *will not* error.

## How Do I Build It?

Use sbt. Test it using the `scripted` task.

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The Play Scraper plugin is in the public domain.  To the extent possible under law, Uri Wilensky has waived all copyright and related or neighboring rights.

[![Hosted By: Cloudsmith](https://img.shields.io/badge/OSS%20hosting%20by-cloudsmith-blue?logo=cloudsmith&style=flat-square)](https://cloudsmith.com)

Package repository hosting is graciously provided by [Cloudsmith](https://cloudsmith.com).
