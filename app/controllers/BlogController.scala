package controllers

import java.io.File

import org.markdown4j.Markdown4jProcessor

import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.Future

case class Post(title: String, content: String)

object BlogController extends Controller {

  val processor: Markdown4jProcessor = new Markdown4jProcessor()

  def frontpage = Action.async { request =>
    val files: Array[File] = Play.getFile("_posts").listFiles()
    val posts = files.toList.map { f =>
      Post(
        title = "Placeholder",
        content = processor.process(f))
    }
    val frontpage = views.html.blog.frontpage(posts)
    Future.successful(Ok(views.html.master(frontpage)))
  }

}
