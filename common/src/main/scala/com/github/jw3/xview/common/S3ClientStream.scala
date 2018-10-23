package com.github.jw3.xview.common

import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage._
import akka.util.ByteString
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, S3ClientOptions}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class S3ClientStream(s3Client: AmazonS3) {
  def multipartUpload(bucket: String, key: String, chunksz: Int = 6291456)(source: Source[ByteString, _])(
    implicit mat: Materializer): Future[CompleteMultipartUploadResult] = {

    import mat.executionContext

    val initUpload = s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key))
    source
      .via(S3ClientStream.rechunk(chunksz))
      .statefulMapConcat { () ⇒ {
        var idx = 0
        bs ⇒ {
          idx += 1
          List(
            new UploadPartRequest()
              .withBucketName(bucket)
              .withKey(key)
              .withUploadId(initUpload.getUploadId)
              .withPartNumber(idx)
              .withPartSize(bs.length)
              .withInputStream(bs.iterator.asInputStream)
          )
        }
      }
      }
      .map(p ⇒ s3Client.uploadPart(p).getPartETag)
      .runWith(Sink.seq)
      .map { etags ⇒
        s3Client.completeMultipartUpload(
          new CompleteMultipartUploadRequest(bucket, key, initUpload.getUploadId, etags.asJava)
        )
      }
  }

}

object S3ClientStream {
  def apply()(implicit cfg: S3Config) = new S3ClientStream(configureClient(cfg))

  def configureClient(cfg: S3Config): AmazonS3Client = {
    val client = cfg.accessKey → cfg.secretKey match {
      case (Some(a), Some(s)) ⇒
        new AmazonS3Client(
          new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(a, s)
          )
        )
      case _ ⇒ new AmazonS3Client
    }

    client.withRegion(cfg.region)
    cfg.endpoint.foreach(client.withEndpoint)
    client.setS3ClientOptions(
      S3ClientOptions.builder.setPathStyleAccess(true).build
    )

    client
  }

  /**
    * Rechunk a stream of bytes according to a chunk size.
    *
    * @param chunkSize the new chunk size
    * @return
    */
  def rechunk(chunkSize: Int) = new GraphStage[FlowShape[ByteString, ByteString]] {
    val in = Inlet[ByteString]("S3Chunker.in")
    val out = Outlet[ByteString]("S3Chunker.out")

    override val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        var buffer = ByteString.empty

        setHandler(
          in,
          new InHandler {
            override def onPush(): Unit = {
              buffer ++= grab(in)
              emitOrPull()
            }

            override def onUpstreamFinish() = if (isAvailable(shape.out)) getHandler(out).onPull()
          }
        )

        setHandler(out, new OutHandler {
          override def onPull(): Unit = emitOrPull()
        })

        def emitOrPull() =
          if (isClosed(in)) {
            if (buffer.isEmpty) completeStage()
            else if (buffer.length < chunkSize) {
              push(out, buffer)
              completeStage()
            } else {
              val (emit, nextBuffer) = buffer.splitAt(chunkSize)
              buffer = nextBuffer
              push(out, emit)
            }
          } else {
            if (buffer.length < chunkSize) pull(in)
            else {
              val (emit, nextBuffer) = buffer.splitAt(chunkSize)
              buffer = nextBuffer
              push(out, emit)
            }
          }
      }
  }
}
