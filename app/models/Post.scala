package models

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat

import reactivemongo.bson.BSONObjectID

object Post {
  implicit val postFormat: Format[Post] = Json.format[Post]
}

case class Post(
  _id: Option[BSONObjectID],
  title: String,
  markdown: String,
  html: String,
  published: Boolean,
  lastUpdateTime: Option[DateTime]) {

  val publishTimeMillis: Option[Long] = _id.map(_.time)
  val publishDate: Option[DateTime] = publishTimeMillis.map(new DateTime(_))
  val publishDateFormatted: Option[String] = publishDate.map(DateTimeFormat.fullDateTime().print(_))
}
