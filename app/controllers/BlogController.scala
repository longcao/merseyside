package controllers

import models.{ Post, PostRepository }

import play.api._
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.templates.Html

import scala.concurrent.Future

import securesocial.core.SecureSocial

trait BlogController extends Controller
  with SecureSocial
  with BlogCrud {

  def editor = SecuredAction {
    val editor: Html = views.html.editor()
    Ok(views.html.master(editor))
  }

  def blog = Action {
    Ok(views.html.master(Html.empty))
  }

  def permalink(id: Long) = Action {
    PostRepository.findById(Option(id)) match {
      case Some(post: Post) =>
        val permalink = views.html.blog.permalink(post)
        Ok(views.html.master(permalink))
      case _ => NotFound(views.html.master(views.html.notfound()))
    }
  }

}

trait BlogCrud {
  self: Controller with SecureSocial =>

  def get(id: Long) = Action.async { request =>
    Future {
      PostRepository.findById(Option(id)) match {
        case Some(post: Post) =>
          Ok(Json.toJson(post))
            .withHeaders(CONTENT_TYPE -> JSON)
        case _ =>
          NotFound(Json.obj())
            .withHeaders(CONTENT_TYPE -> JSON)
      }
    }
  }

  def save = SecuredAction(ajaxCall = true)(parse.json) { implicit request =>
    val post: Post = request.body.as[Post]
    val savedPost: Post = PostRepository.save(post)
    Ok(Json.toJson(savedPost))
      .withHeaders(CONTENT_TYPE -> JSON)
  }
}

object BlogController extends BlogController
