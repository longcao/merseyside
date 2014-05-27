package controllers

import models.{ Post, PostRepository }

import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.templates.Html

import securesocial.core.SecureSocial

object Application extends Controller with SecureSocial {

  def home = Action {
    val content: Html = views.html.content()
    Ok(views.html.master(content))
  }

  def blog = Action {
    val content: Html = views.html.content()
    Ok(views.html.master(content))
  }

  def about = Action {
    val content: Html = views.html.about()
    Ok(views.html.master(content))
  }

  def editor = SecuredAction {
    val editor: Html = views.html.editor()
    Ok(views.html.master(editor))
  }

  def save = Action(parse.json) { implicit request =>
    val post: Post = request.body.as[Post]
    val savedPost: Post = PostRepository.save(post)
    Ok(Json.toJson(savedPost).toString)
  }

}