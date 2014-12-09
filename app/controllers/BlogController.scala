package controllers

import collections.PostCollection

import models.Post

import org.joda.time.DateTime

import play.api._
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._
import play.twirl.api.Html

import scala.concurrent.Future

import utils.Hasher

object BlogController extends Controller with PostCollection {

  def editor(slug: Option[String]) = Action.async { request =>
    val editor: Html = views.html.editor(slug)
    Future.successful(Ok(views.html.master(editor)))
  }

  def frontpage = Action.async { request =>
    val query = Json.obj("published" -> true)
    val sort = Json.obj("lastUpdateTime" -> -1)

    postCollection.find(query)
      .sort(sort)
      .cursor[Post]
      .collect[List](upTo = 5)
      .map { posts =>
        val frontpage = views.html.blog.frontpage(posts)
        Ok(views.html.master(frontpage))
      }
  }

  def permalink(slug: String) = Action.async { request =>
    postCollection.find(Json.obj("slug" -> slug))
      .one[Post]
      .map { _ match {
        case Some(post: Post) =>
          val permalink = views.html.blog.permalink(post)
          Ok(views.html.master(permalink))
        case _ => NotFound(views.html.master(views.html.notfound()))
      }
    }
  }

  def permalinkWithTitle(id: String, title: String) = permalink(id)

  def get(slug: String) = Action.async { request =>
    postCollection.find(Json.obj("slug" -> slug))
      .one[Post]
      .map { _ match {
        case Some(post) => Ok(Json.toJson(post)).withHeaders(CONTENT_TYPE -> JSON)
        case None => NotFound
      }
    }
  }

  def save = Action.async(parse.json) { request =>
    val post: Post = request.body.as[Post]

    val slug: String = post.slug.getOrElse(Hasher.generateNewSlug)

    val postJson: JsValue = Json.toJson(
      post.copy(
        slug = Some(slug),
        lastUpdateTime = Some(DateTime.now())))

    postCollection.update(
      selector = Json.obj("slug" -> slug),
      update = postJson,
      upsert = true
    ).map { lastError =>
      if (!lastError.ok) {
        Logger.info(s"Mongo LastError: $lastError")
        InternalServerError("Error saving post")
      } else {
        Ok(postJson).withHeaders(CONTENT_TYPE -> JSON)
      }
    }
  }

}
