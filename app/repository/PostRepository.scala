package repository

import com.github.tototoshi.slick.MySQLJodaSupport._

import models.Post

import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._

object PostRepository extends Repository {
  class Posts(tag: Tag) extends Table[Post](tag, "posts") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def markdown = column[String]("markdown")
    def html = column[String]("html")
    def published = column[Boolean]("published")
    def creationTime = column[DateTime]("creationTime")
    def lastUpdateTime = column[DateTime]("lastUpdateTime")

    def * = (id.?, title.?, markdown, html, published, creationTime.?, lastUpdateTime.?) <> ((Post.apply _).tupled, Post.unapply)
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

