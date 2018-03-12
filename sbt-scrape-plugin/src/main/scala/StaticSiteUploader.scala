package org.nlogo

import
  com.amazonaws.{ auth, regions, services },
    auth.{ AWSCredentialsProvider, profile },
      profile.ProfileCredentialsProvider,
    regions.Regions,
    services.{ cloudfront, s3 },
      s3.{ AmazonS3, AmazonS3Client, AmazonS3ClientBuilder, model => s3model },
        s3model.{ AccessControlList, GetObjectMetadataRequest, GroupGrantee, ObjectMetadata,
                  Permission, PutObjectRequest, RedirectRule, RoutingRule, RoutingRuleCondition },
      cloudfront.{ AmazonCloudFrontClient, AmazonCloudFrontClientBuilder, model => cfmodel },
        cfmodel.{ CreateInvalidationRequest, InvalidationBatch, Paths }

import
  java.io.{ File, FileInputStream, IOException }

import
  org.apache.commons.codec.digest.DigestUtils

import
  scala.{ collection, concurrent, util },
    collection.JavaConverters._,
    concurrent.{ Await, Future, ExecutionContext, duration },
      ExecutionContext.Implicits.global,
      duration.Duration,
    util.Try


object StaticSiteUploader {
  def deploy(
    credential:      AWSCredentialsProvider,
    regionName:      String,
    targetDirectory: File,
    bucketId:        String,
    distributionId:  Option[String],
    redirectHost:    Option[String]
  ) = {
    lazy val allFiles =
      recursiveFileEnumeration(targetDirectory)
        .filter(excludeFiles)

    val region = Regions.fromName(regionName)

    lazy val fileKeys = allFiles.map(fileToKey(targetDirectory))

    lazy val metadataRequests =
      fileKeys.map(getMetadataRequest(bucketId))

    lazy val putRequests =
      (fileKeys zip allFiles).map((putRequest(bucketId) _).tupled)

    val client: AmazonS3Client =
      AmazonS3ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(credential)
        .build().asInstanceOf[AmazonS3Client]

    upload(metadataRequests zip putRequests, client)

    addRedirects(bucketId, fileKeys, redirectHost, client)

    distributionId.foreach(id => invalidateDistribution(id, region, credential))
  }

  def upload(requests: Seq[(GetObjectMetadataRequest, PutObjectRequest)], client: AmazonS3Client) = {
    val d = Duration("30 minutes")
    println("beginning upload")
    val futureRequests = requests.map {
      case (mdr, pr) => Future { attemptPut(client, pr)(attemptMetadata(client, mdr)) }
    }
    val sequencedRequests = Future.sequence(futureRequests)
    val putResults = Await.result(sequencedRequests, d)
    println("finished upload")
    putResults.filter(! _.isSuccess).foreach { pr =>
      println("FAILED TO UPLOAD: " + pr.failed.get.getMessage)
    }
  }

  def addRedirects(bucketId: String, fileKeys: Seq[String], redirectHost: Option[String], client: AmazonS3Client) = {
    try {
      val redirectToAppropriateHost: RedirectRule => RedirectRule =
        redirectHost.map(host => (r: RedirectRule) => r.withHostName(host)).getOrElse(identity _)
      val routingRules = fileKeys
        .filter(! _.contains("."))
        .map(k =>
            new RoutingRule()
              .withCondition(new RoutingRuleCondition().withKeyPrefixEquals(s"$k/"))
              .withRedirect(redirectToAppropriateHost(new RedirectRule().withReplaceKeyWith(k))))
      val currentConfiguration = client.getBucketWebsiteConfiguration(bucketId)
      client.setBucketWebsiteConfiguration(bucketId, currentConfiguration.withRoutingRules(routingRules.asJava))
    } catch {
      case e: Throwable =>
        println("failed to add redirects:")
        println(e.getMessage)
        e.printStackTrace()
    }
  }

  def attemptPut(client: AmazonS3Client, request: PutObjectRequest)(currentObjectMetadata: Try[ObjectMetadata]): Try[Unit] =
    for {
      metadata <- currentObjectMetadata
    } yield
      if (metadata.getUserMetadata.asScala.getOrElse("sha1", "") != request.getMetadata.getUserMetadata.get("sha1"))
        Try { client.putObject(request) }
      else Try { () }

  def attemptMetadata(client: AmazonS3Client, request: GetObjectMetadataRequest): Try[ObjectMetadata] =
    Try { client.getObjectMetadata(request) }.recover {
      case e: Throwable => new ObjectMetadata()
    }

  def invalidateDistribution(distributionId: String, region: Regions, credentialsProvider: AWSCredentialsProvider) = {
    val cfc = AmazonCloudFrontClientBuilder.standard()
      .withCredentials(credentialsProvider)
      .withRegion(region)
      .asInstanceOf[AmazonCloudFrontClient]
    val paths = new Paths().withItems("/*").withQuantity(1)
    val ib = new InvalidationBatch(paths, java.lang.System.currentTimeMillis.toString) // the timestring must be unique
    val cir = new CreateInvalidationRequest(distributionId, ib)
    println("Invalidating")
    cfc.createInvalidation(cir)
    println("Invalidation finished")
  }

  def versionMetadata(f: File) = {
    val md = new ObjectMetadata()
    val computedSHA1 = DigestUtils.sha1Hex(new FileInputStream(f))
    md.addUserMetadata("sha1", computedSHA1)
    md
  }

  def htmlMetadata(md: ObjectMetadata): ObjectMetadata = {
    md.setContentType("text/html; charset=utf-8")
    md
  }

  def fileToKey(targetDirectory: File)(f: File): String =
    f.getCanonicalPath.drop(s"${targetDirectory.getCanonicalPath}/".length)

  val publiclyReadable: AccessControlList = {
    val acl = new AccessControlList()
    acl.grantPermission(GroupGrantee.AllUsers, Permission.Read)
    acl
  }

  def recursiveFileEnumeration(f: File): Seq[File] =
    if (f.isDirectory)
      f.listFiles.flatMap(recursiveFileEnumeration)
    else
      Seq(f)

  val excludeFiles: (File) => Boolean = (file: File) => ! file.getName.startsWith(".")

  def getMetadataRequest(bucketName: String)(key: String): GetObjectMetadataRequest =
    new GetObjectMetadataRequest(bucketName, key)

  def putRequest(bucketName: String)(key: String, f: File): PutObjectRequest = {
    val pr = new PutObjectRequest(bucketName, key, f)
    if (f.getName.endsWith("html") || ! f.getName.contains('.'))
      pr.withMetadata(htmlMetadata(versionMetadata(f))).withAccessControlList(publiclyReadable)
    else
      pr.withMetadata(versionMetadata(f)).withAccessControlList(publiclyReadable)
  }
}
