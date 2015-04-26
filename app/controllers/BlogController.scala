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
    val posts = PostService.allPosts.toList.sorted(Post.postOrdering)
    val frontpage = views.html.blog.frontpage(posts)
    Future.successful(Ok(views.html.master(frontpage)))
  }

  def postsByTag(tag: String) = Action.async { request =>
    PostService.loadByTag(tag).toList match {
      case Nil => notFound
      case posts =>
        val tagPage = views.html.blog.frontpage(posts)
        Future.successful(Ok(views.html.master(tagPage)))
    }
  }

  def permalink(year: Int, month: Int, day: Int, title: String) = Action.async { request =>
    PostService.posts.get(request.path) match {
      case Some(post) =>
        val perma = views.html.blog.permalink(post)
        Future.successful {
          Ok(views.html.master(perma, post.title))
        }
      case _ => notFound
    }
  }

  private def notFound: Future[Result] = Future.successful {
    NotFound(views.html.master(views.html.notfound()))
  }

}
