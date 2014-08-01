package models

import org.joda.time.DateTime

import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat

import reactivemongo.bson.BSONObjectID

object Post {
  implicit val postFormat: Format[Post] = Json.format[Post]
}

case class Post(
  _id: Option[BSONObjectID],
  title: Option[String],
  markdown: String,
  html: String,
  published: Boolean,
  creationTime: Option[DateTime],
  lastUpdateTime: Option[DateTime])
