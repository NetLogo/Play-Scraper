package org.nlogo

import
  com.amazonaws.{ auth, services },
    auth.{ AWSCredentialsProvider, profile },
      profile.ProfileCredentialsProvider,
    services.{ cloudfront, s3 },
      s3.{ AmazonS3, AmazonS3Client, model => s3model },
        s3model.{ AccessControlList, GetObjectMetadataRequest, GroupGrantee, ObjectMetadata, Permission, PutObjectRequest },
      cloudfront.{ AmazonCloudFrontClient, model => cfmodel },
        cfmodel.{ CreateInvalidationRequest, InvalidationBatch, Paths }

import
  java.io.{ File, FileInputStream, IOException }

import
  org.apache.commons.codec.digest.DigestUtils

import
  scala.{ collection, concurrent, util },
    collection.JavaConversions._,
    concurrent.{ Await, Future, ExecutionContext, duration },
      ExecutionContext.Implicits.global,
      duration.Duration,
    util.Try


object StaticSiteUploader {
  def deploy(
    credential:      AWSCredentialsProvider,
    targetDirectory: File,
    bucketId:        String,
    distributionId:  Option[String]
  ) = {
    lazy val allFiles =
      recursiveFileEnumeration(targetDirectory)
        .filter(excludeFiles)

    lazy val keysAndFiles =
      allFiles.map(fileToKey(targetDirectory)) zip allFiles

    lazy val metadataRequests =
      keysAndFiles.map(_._1).map(getMetadataRequest(bucketId))

    lazy val putRequests =
      keysAndFiles.map((putRequest(bucketId) _).tupled)

    upload(metadataRequests zip putRequests, credential)

    distributionId.foreach(id => invalidateDistribution(id, credential))
  }

  def attemptPut(client: AmazonS3Client, request: PutObjectRequest)(currentObjectMetadata: Try[ObjectMetadata]): Try[Unit] =
    for {
      metadata <- currentObjectMetadata
    } yield
      if (metadata.getUserMetadata.getOrElse("sha1", "") != request.getMetadata.getUserMetadata.get("sha1"))
        Try { client.putObject(request) }
      else Try { () }

  def attemptMetadata(client: AmazonS3Client, request: GetObjectMetadataRequest): Try[ObjectMetadata] =
    Try { client.getObjectMetadata(request) }.recover {
      case e: Throwable => new ObjectMetadata()
    }

  def upload(requests: Seq[(GetObjectMetadataRequest, PutObjectRequest)], credentialsProvider: AWSCredentialsProvider) = {
    val d = Duration("30 minutes")
    val client = new AmazonS3Client(credentialsProvider)
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

  def invalidateDistribution(distributionId: String, credentialsProvider: AWSCredentialsProvider) = {
    val cfc = new AmazonCloudFrontClient(credentialsProvider)
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
