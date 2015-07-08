package org.nlogo

import
  com.amazonaws.{ auth, services },
    auth.{ AWSCredentialsProvider, profile },
      profile.ProfileCredentialsProvider,
    services.{ cloudfront, s3 },
      s3.{ AmazonS3, AmazonS3Client, model => s3model },
        s3model.{ AccessControlList, GroupGrantee, ObjectMetadata, Permission, PutObjectRequest },
      cloudfront.{ AmazonCloudFrontClient, model => cfmodel },
        cfmodel.{ CreateInvalidationRequest, InvalidationBatch, Paths }

import
  java.io.{ File, IOException }

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
    lazy val allPutRequests =
      recursiveFileEnumeration(targetDirectory)
        .filter(excludeFiles)
        .map(putRequest(bucketId))

    upload(allPutRequests, credential)

    distributionId.foreach(id => invalidateDistribution(id, credential))
  }

  def upload(putRequests: Seq[PutObjectRequest], credentialsProvider: AWSCredentialsProvider) = {
    val d = Duration("30 minutes")
    val s3client = new AmazonS3Client(credentialsProvider)
    println("beginning upload")
    val futurePutRequests = putRequests.map(r => Future { Try {
      try {
        s3client.putObject(r)
      } catch {
        case e: Throwable =>
          println("FAILED TO UPLOAD: " + r.getKey)
          throw e
      }
    }
    })
    val putRequestsFuture = Future.sequence(futurePutRequests)
    val putResults = Await.result(putRequestsFuture, d)
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

  def htmlMetadata = {
    val hm = new ObjectMetadata()
    hm.setContentType("text/html; charset=utf-8")
    hm
  }

  def fileToKey(f: File): String = f.getPath.split("/").drop(2).mkString("/")

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

  def putRequest(bucketName: String)(f: File): PutObjectRequest = {
    val pr = new PutObjectRequest(bucketName, fileToKey(f), f)
    if (f.getName.endsWith("html") || ! f.getName.contains('.'))
      pr.withMetadata(htmlMetadata).withAccessControlList(publiclyReadable)
    else
      pr.withAccessControlList(publiclyReadable)
  }
}

case class PublicationConfig(
)
