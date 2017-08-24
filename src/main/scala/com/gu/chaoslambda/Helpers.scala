package com.gu.chaoslambda

import com.amazonaws.services.ec2.model.Tag

object Helpers {

  import scala.collection.JavaConverters._

  def extractTagValue(tags: java.util.List[Tag], tagKey: String): Option[String] = tags.asScala.find(_.getKey == tagKey).map(_.getValue)

}
