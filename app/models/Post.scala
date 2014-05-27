package models

import com.github.tototoshi.slick.MySQLJodaSupport._

import org.joda.time.DateTime

import play.api.libs.json._

import scala.slick.driver.MySQLDriver.simple._

object PostRepository extends Repository {
  class Posts(tag: Tag) extends Table[Post](tag, "posts") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def content = column[String]("content")
    def creationTime = column[DateTime]("creationTime")

    def * = (id.?, title.?, content, creationTime.?) <> ((Post.apply _).tupled, Post.unapply)
  }
  val posts = TableQuery[Posts]

  def findById(idOpt: Option[Long]): Option[Post] = Database.forDataSource(ds) withSession { implicit session =>
    idOpt flatMap { id =>
      val q = for {
        p <- posts if p.id is id
      } yield p
      q.firstOption
    }
  }

  def save(post: Post): Post = Database.forDataSource(ds) withSession { implicit session =>
    findById(post.id) match {
      case None =>
        val id = (posts returning posts.map(_.id)).insert(post)
        post.copy(id = Option(id))
      case Some(existingPost) =>
        val q = for {
          p <- posts
          if p.id is existingPost.id
        } yield p

        val updatedPost = post.copy(id = existingPost.id)
        q.update(updatedPost)
        updatedPost
    }
  }
}

object Post {
  implicit val postFormat: Format[Post] = new Format[Post] {
    def reads(json: JsValue): JsResult[Post] = json match {
      case _: JsObject => JsSuccess(Post(
        id = (json \ "id").asOpt[Long],
        title = (json \ "title").asOpt[String],
        content = (json \ "content").as[String],
        creationTime = (json \ "creationTime").asOpt[DateTime]))
      case _ => JsError("Invalid JSON supplied.")
    }

    def writes(post: Post): JsObject = Json.obj(
      "id" -> post.id,
      "title" -> post.title,
      "content" -> post.content,
      "creationTime" -> post.creationTime)
  }
}

case class Post(
  id: Option[Long],
  title: Option[String],
  content: String,
  creationTime: Option[DateTime])
