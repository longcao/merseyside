package repository

import com.github.tototoshi.slick.MySQLJodaSupport._

import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._

import securesocial.core.{ Authenticator, IdentityId }

object AuthenticatorRepository extends Repository {
  class Authenticators(tag: Tag) extends Table[Authenticator](tag, "authenticators") {
    def id = column[String]("id")
    def userId = column[String]("userId")
    def providerId = column[String]("providerId")
    def creationDate = column[DateTime]("creationDate")
    def lastUsed = column[DateTime]("lastUsed")
    def expirationDate = column[DateTime]("expirationDate")

    def * = (id, userId, providerId, creationDate, lastUsed, expirationDate).shaped <> (
      tuple => Authenticator(tuple._1, IdentityId(tuple._2, tuple._3), tuple._4, tuple._5, tuple._6),
      (a: Authenticator) => Option((a.id, a.identityId.userId, a.identityId.providerId, a.creationDate, a.lastUsed, a.expirationDate)))
  }
  val authenticators = TableQuery[Authenticators]

  def findById(id: String): Option[Authenticator] = Database.forDataSource(ds) withSession { implicit session =>
    val q = for {
      authenticator <- authenticators
      if authenticator.id is id
    } yield authenticator
    q.firstOption
  }

  def save(authenticator: Authenticator): Authenticator = {
    Database.forDataSource(ds) withSession { implicit session =>
      findById(authenticator.id) match {
        case None => {
          authenticators.insert(authenticator)
          authenticator
        }
        case Some(existingAuthenticator) => {
          val authenticatorRow = for {
            a <- authenticators
            if a.id is existingAuthenticator.id
          } yield a

          val updatedAuthenticator = authenticator.copy(id = existingAuthenticator.id)
          authenticatorRow.update(updatedAuthenticator)
          updatedAuthenticator
        }
      }
    }
  }

  def delete(id: String) = Database.forDataSource(ds) withSession { implicit session =>
    val q = for {
      a <- authenticators
      if a.id is id
    } yield a
    q.delete
  }
}
