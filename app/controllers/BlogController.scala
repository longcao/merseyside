package controllers

import model.Post

import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.Future

import service.PostService

object BlogController extends Controller {

  def frontpage = Action.async { request =>
    val frontpage = views.html.blog.frontpage(PostService.posts)
    Future.successful(Ok(views.html.master(frontpage)))
  }

  def permalink(year: Int, month: Int, day: Int, title: String) = Action.async { request =>
    val post = Post(
      title = "Placeholder",
      permalink = "/2014/05/12/placeholder-title",
      content = "content")

    val perma = views.html.blog.permalink(post)
    Future.successful(Ok(views.html.master(perma)))
  }

}
