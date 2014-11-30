package controllers

import models.Post

import org.joda.time.DateTime

import play.api._
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection.JSONCollection
import play.twirl.api.Html

import reactivemongo.api._
import reactivemongo.api.indexes._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future
import scala.util.{ Failure, Success }

import utils.Hasher

object BlogController extends Controller with MongoController {

  def collection: JSONCollection = db.collection[JSONCollection]("posts")

  collection.indexesManager.ensure(Index(
    Seq(
      "slug" -> IndexType.Ascending)))

  def editor = Action {
    val editor: Html = views.html.editor()
    Ok(views.html.master(editor))
  }

  def frontpage = Action.async { request =>
    val query = Json.obj("published" -> true)
    val sort = Json.obj("lastUpdateTime" -> -1)

    collection.find(query)
      .sort(sort)
      .cursor[Post]
      .collect[List](upTo = 5)
      .map { posts =>
        val frontpage = views.html.blog.frontpage(posts)
        Ok(views.html.master(frontpage))
      }
  }

  def permalink(slug: String) = Action.async { request =>
    collection.find(Json.obj("slug" -> slug))
      .one[Post]
      .map { _ match {
        case Some(post: Post) =>
          val permalink = views.html.blog.permalink(post)
          Ok(views.html.master(permalink))
        case _ => NotFound(views.html.master(views.html.notfound()))
      }
    }
  }

  def permalinkWithTitle(id: String, title: String) = permalink(id)

  def get(slug: String) = Action.async { request =>
    collection.find(Json.obj("slug" -> slug))
      .one[Post]
      .map { _ match {
        case Some(post) => Ok(Json.toJson(post)).withHeaders(CONTENT_TYPE -> JSON)
        case None => NotFound
      }
    }
  }

  def save = Action.async(parse.json) { request =>
    val post: Post = request.body.as[Post]

    val slug: String = post.slug.getOrElse(Hasher.generateNewSlug)

    val postJson: JsValue = Json.toJson(
      post.copy(
        slug = Some(slug),
        lastUpdateTime = Some(DateTime.now())))

    collection.update(
      selector = Json.obj("slug" -> slug),
      update = postJson,
      upsert = true
    ).map { lastError =>
      if (!lastError.ok) {
        Logger.info(s"Mongo LastError: $lastError")
        InternalServerError("Error saving post")
      } else {
        Ok(postJson).withHeaders(CONTENT_TYPE -> JSON)
      }
    }
  }

}
