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
    def published = column[Boolean]("published")
    def creationTime = column[DateTime]("creationTime")
    def lastUpdateTime = column[DateTime]("lastUpdateTime")

    def * = (id.?, title.?, content, published, creationTime.?, lastUpdateTime.?) <> ((Post.apply _).tupled, Post.unapply)
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
        (posts returning posts.map(_.id)).into {
          (post, id) =>
            post.copy(id = Option(id))
        }.insert(
          post.copy(creationTime = Some(new DateTime())))
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
        published = (json \ "published").as[Boolean],
        creationTime = (json \ "creationTime").asOpt[DateTime],
        lastUpdateTime = (json \ "lastUpdateTime").asOpt[DateTime]))
      case _ => JsError("Invalid JSON supplied.")
    }

    def writes(post: Post): JsObject = Json.obj(
      "id" -> post.id,
      "title" -> post.title,
      "content" -> post.content,
      "published" -> post.published,
      "creationTime" -> post.creationTime,
      "lastUpdateTime" -> post.lastUpdateTime)
  }
}

case class Post(
  id: Option[Long],
  title: Option[String],
  content: String,
  published: Boolean,
  creationTime: Option[DateTime],
  lastUpdateTime: Option[DateTime])
