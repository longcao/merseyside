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
  slug: Option[String],
  title: String,
  markdown: String,
  html: String,
  published: Boolean,
  lastUpdateTime: Option[DateTime]) {

  lazy val urlifiedTitle: String =
    title.replaceAll("[^A-Za-z0-9]", "-") // only allow alphanumerics
      .replaceAll("-+", "-") // no extra dashes
      .toLowerCase

  val createTimeMillis: Option[Long] = _id.map(_.time)
  val createDate: Option[DateTime] = createTimeMillis.map(new DateTime(_))
  val createDateFormatted: Option[String] = createDate.map(DateTimeFormat.fullDateTime().print(_))
}
