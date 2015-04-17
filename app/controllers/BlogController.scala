package controllers

import java.io.File

import org.markdown4j.Markdown4jProcessor

import model.Post

import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.Future

object BlogController extends Controller {

  val processor: Markdown4jProcessor = new Markdown4jProcessor()

  def frontpage = Action.async { request =>
    val files: Array[File] = Play.getFile("_posts").listFiles()
    val posts = files.toList.map { f =>
      Post(
        title = "Placeholder",
        permalink = "/2014/05/12/placeholder-title",
        content = processor.process(f))
    }
    val frontpage = views.html.blog.frontpage(posts)
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
