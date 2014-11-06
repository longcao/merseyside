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
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object BlogController extends Controller with MongoController {

  def collection: JSONCollection = db.collection[JSONCollection]("posts")

  def editor = Action {
    val editor: Html = views.html.editor()
    Ok(views.html.master(editor))
  }

  def frontpage = Action.async { request =>
    val query = Json.obj("published" -> true)
    collection.find(query)
      .cursor[Post]
      .collect[List]()
      .map { posts =>
        val frontpage = views.html.blog.frontpage(posts)
        Ok(views.html.master(frontpage))
      }
  }

  def permalink(id: String) = Action.async { request =>
    BSONObjectID.parse(id) match {
      case Success(parsedId) =>
        collection.find(Json.obj("_id" -> parsedId))
          .one[Post]
          .map { _ match {
            case Some(post: Post) =>
              val permalink = views.html.blog.permalink(post)
              Ok(views.html.master(permalink))
            case _ => NotFound(views.html.master(views.html.notfound()))
          }
        }
      case Failure(ex) => Future.successful(NotFound(views.html.master(views.html.notfound())))
    }
  }

  def permalinkWithTitle(id: String, title: String) = permalink(id)

  def get(id: String) = Action.async { request =>
    BSONObjectID.parse(id) match {
      case Success(parsedId) =>
        collection.find(Json.obj("_id" -> BSONObjectID(id)))
          .one[Post]
          .map { _ match {
            case Some(post) => Ok(Json.toJson(post)).withHeaders(CONTENT_TYPE -> JSON)
            case None => NotFound
          }
        }
      case Failure(ex) => Future.successful(NotFound)
    }
  }

  def save = Action.async(parse.json) { implicit request =>
    val post: Post = request.body.as[Post]

    val postJson: JsValue = Json.toJson(
      post.copy(lastUpdateTime = Some(DateTime.now())))

    collection.insert(postJson).map { lastError =>
      if (!lastError.ok) {
        Logger.info(s"Mongo LastError: $lastError")
        InternalServerError("Error saving post")
      } else {
        Ok(postJson).withHeaders(CONTENT_TYPE -> JSON)
      }
    }
  }
}
