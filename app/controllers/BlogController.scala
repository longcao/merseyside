package controllers

import model.Post

import play.api._
import play.api.mvc._
import play.api.Play.current

import service.PostService

object BlogController extends Controller {

  lazy val sortedPosts = PostService.allPosts.toList.sorted(Post.postOrdering)

  def frontpage = Action { request =>
    val frontpage = views.html.blog.frontpage(sortedPosts)

    Ok(views.html.master(frontpage))
  }

  def postsByTag(tag: String) = Action { request =>
    PostService.loadByTag(tag).toList match {
      case Nil => notFound
      case posts =>
        val tagPage = views.html.blog.frontpage(posts)
        Ok(views.html.master(tagPage))
    }
  }

  def permalink(year: Int, month: Int, day: Int, title: String) = Action { request =>
    PostService.posts.get(request.path) match {
      case Some(post) =>
        val perma = views.html.blog.permalink(post)
        Ok(views.html.master(perma, post.title))
      case _ => notFound
    }
  }

  def redirectFeed = Action { request =>
    if (request.path == "/rss.xml")
      Redirect(routes.BlogController.rss)
    else if (request.path == "/atom.xml")
      Redirect(routes.BlogController.atom)
    else
      notFound
  }

  def rss = Action {
    Ok(views.xml.blog.feeds.rss(sortedPosts))
  }

  def atom = Action {
    Ok(views.xml.blog.feeds.atom(sortedPosts))
  }

  private def notFound: Result =
    NotFound(views.html.master(views.html.notfound()))
}
