package repository

import com.github.tototoshi.slick.MySQLJodaSupport._

import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._

import securesocial.core.providers.Token

object TokenRepository extends Repository {
  class Tokens(tag: Tag) extends Table[Token](tag, "tokens") {
    def uuid = column[String]("uuid")
    def email = column[String]("email")
    def creationTime = column[DateTime]("creationTime")
    def expirationTime = column[DateTime]("expirationTime")
    def isSignUp = column[Boolean]("isSignUp")

    def * = (uuid, email, creationTime, expirationTime, isSignUp) <> (Token.tupled, Token.unapply)
  }
  val tokens = TableQuery[Tokens]

  def findById(uuid: String): Option[Token] = Database.forDataSource(ds) withSession { implicit session =>
    val q = for {
      t <- tokens
      if t.uuid is uuid
    } yield t
    q.firstOption
  }

  def save(token: Token): Token = Database.forDataSource(ds) withSession { implicit session =>
    findById(token.uuid) match {
      case None => {
        tokens.insert(token)
        token
      }
      case Some(existingToken) => {
        val tokenRow = for {
          t <- tokens
          if t.uuid is existingToken.uuid
        } yield t

        val updatedToken = token.copy(uuid = existingToken.uuid)
        tokenRow.update(updatedToken)
        updatedToken
      }
    }
  }

  def delete(uuid: String) = Database.forDataSource(ds) withSession { implicit session =>
    val q = for {
      t <- tokens
      if t.uuid is uuid
    } yield t
    q.delete
  }

  def deleteExpiredTokens(currentDate: DateTime) = Database.forDataSource(ds) withSession { implicit session =>
    val q = for {
      t <- tokens
      if t.expirationTime < currentDate
    } yield t
    q.delete
  }
}
