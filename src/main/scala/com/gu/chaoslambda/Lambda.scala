package com.gu.chaoslambda

import com.amazonaws.auth.{ AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import org.slf4j.{ Logger, LoggerFactory }
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model._
import com.amazonaws.regions.Regions
import scala.collection.JavaConverters._
import Helpers.extractTagValue

/*
{
  "stage": "CODE",
  "stack": "frontend"
}
*/

class Input() {
  var stage: String = _
  var stack: String = _

  def getStage: String = stage
  def setStage(s: String): Unit = stage = s

  def getStack: String = stack
  def setStack(s: String): Unit = stack = s

}

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val credentials = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("frontend"),
    DefaultAWSCredentialsProviderChain.getInstance())

  val ec2Client = AmazonEC2ClientBuilder.standard()
    .withCredentials(credentials)
    .withRegion(Regions.EU_WEST_1)
    .build()

  def handler(input: Input): Unit = {

    val r = scala.util.Random

    val request = new DescribeInstancesRequest()
    val response = ec2Client.describeInstances(request)

    val instances = for {
      reservations <- response.getReservations.asScala
      instance <- reservations.getInstances.asScala
    } yield instance

    instances.toList.foreach { instance =>
      val tags = instance.getTags
      val appTag = extractTagValue(tags, "App").getOrElse("")
      val stackTag = extractTagValue(tags, "Stack").getOrElse("")
      val stageTag = extractTagValue(tags, "Stage").getOrElse("")
      val change = r.nextInt(100)

      if (change >= 90 && stageTag == input.stage && stackTag == input.stack) {
        logger.info(s"Killing instance: ${instance.getInstanceId}, env: $stageTag, stack: $stackTag, app: $appTag")
        //val terminationRequest = new TerminateInstancesRequest(List(instance.getInstanceId).asJava)
        //ec2Client.terminateInstances(terminationRequest)
      } else {
        logger.info(s"Instance: ${instance.getInstanceId}, env: $stageTag, stack: $stackTag, app: $appTag")
      }
    }
  }
}

object TestIt {
  def main(args: Array[String]): Unit = {
    val targets: Input = new Input()
    targets.setStage("CODE")
    targets.setStack("frontend")
    Lambda.handler(targets)
  }
}
