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

object BlogController extends Controller with MongoController {

  def editor = Action {
    val editor: Html = views.html.editor()
    Ok(views.html.master(editor))
  }

  def blog = Action {
    Ok(views.html.master(Html("")))
  }

  /*
  def permalink(id: Long) = Action {
    Option(id) match {
      case Some(post: Post) =>
        val permalink = views.html.blog.permalink(post)
        Ok(views.html.master(permalink))
      case _ => NotFound(views.html.master(views.html.notfound()))
    }
  }
  */

  def get(id: String) = Action.async { request =>
    collection.find(Json.obj("_id" -> BSONObjectID(id)))
      .one[Post]
      .map { _ match {
        case Some(post) => Ok(Json.toJson(post)).withHeaders(CONTENT_TYPE -> JSON)
        case None => NotFound
      }
    }
  }

  def collection: JSONCollection = db.collection[JSONCollection]("posts")

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
