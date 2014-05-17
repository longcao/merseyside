package models

import javax.sql.DataSource

import play.api.db.DB
import play.api.Play.current

import scala.slick.driver.MySQLDriver.simple._

object UserRepository {
  class Users(tag: Tag) extends Table[User](tag, "users") {
    def username = column[String]("username")
    def password = column[String]("password")
    def * = (username, password) <> (User.tupled, User.unapply) 
  }
  val users = TableQuery[Users]

  val ds: DataSource = DB.getDataSource()

  def loadUserByName(name: String): Option[User] = {
    Database.forDataSource(ds) withSession { implicit session =>
      users.filter(_.username === name).list.headOption
    }
  }
}

case class User(
  username: String,
  password: String)
