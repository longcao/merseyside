package models

import org.joda.time.DateTime

import play.api.libs.json._

object Post {
  implicit val postFormat: Format[Post] = new Format[Post] {
    def reads(json: JsValue): JsResult[Post] = json match {
      case _: JsObject => JsSuccess(Post(
        id = (json \ "id").asOpt[Long],
        title = (json \ "title").asOpt[String],
        markdown = (json \ "markdown").as[String],
        html = (json \ "html").as[String],
        published = (json \ "published").as[Boolean],
        creationTime = (json \ "creationTime").asOpt[DateTime],
        lastUpdateTime = (json \ "lastUpdateTime").asOpt[DateTime]))
      case _ => JsError("Invalid JSON supplied.")
    }

    def writes(post: Post): JsObject = Json.obj(
      "id" -> post.id,
      "title" -> post.title,
      "markdown" -> post.markdown,
      "html" -> post.html,
      "published" -> post.published,
      "creationTime" -> post.creationTime,
      "lastUpdateTime" -> post.lastUpdateTime)
  }
}

case class Post(
  id: Option[Long],
  title: Option[String],
  markdown: String,
  html: String,
  published: Boolean,
  creationTime: Option[DateTime],
  lastUpdateTime: Option[DateTime])
