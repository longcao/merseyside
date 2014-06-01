package controllers

import models.{ Post, PostRepository }

import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.templates.Html

import securesocial.core.SecureSocial

object Application extends Controller with SecureSocial {

  def home = Action {
    Ok(views.html.master(Html.empty))
  }

  def about = Action {
    val about: Html = views.html.about()
    Ok(views.html.master(about))
  }

  def editor = SecuredAction {
    val editor: Html = views.html.editor()
    Ok(views.html.master(editor))
  }

  def blog = Action {
    Ok(views.html.master(Html.empty))
  }

  def save = Action(parse.json) { implicit request =>
    val post: Post = request.body.as[Post]
    val savedPost: Post = PostRepository.save(post)
    Ok(Json.toJson(savedPost).toString)
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