package models

import javax.sql.DataSource

import play.api.db.DB
import play.api.Play.current

import securesocial.core._

import scala.slick.driver.MySQLDriver.simple._

trait CustomColumnTypes {
  implicit val authenticationMethodColumnType =
    MappedColumnType.base[AuthenticationMethod, String](
      (authenticationMethod: AuthenticationMethod) => authenticationMethod.method,
      (string: String) => AuthenticationMethod(string))
}

object UserRepository extends CustomColumnTypes {
  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    // IdentityId
    def userId = column[String]("userId")
    def providerId = column[String]("providerId")

    def firstName = column[String]("firstName")
    def lastName = column[String]("lastName")
    def fullName = column[String]("fullName")
    def email = column[Option[String]]("email")
    def avatarUrl = column[Option[String]]("avatarUrl")

    def authMethod = column[AuthenticationMethod]("authMethod")

    // passwordInfo
    def hasher = column[Option[String]]("hasher")
    def password = column[Option[String]]("password")
    def salt = column[Option[String]]("salt")

    def * = {
      val shaped = (
        id.?,
        userId,
        providerId,
        firstName,
        lastName,
        fullName,
        email,
        avatarUrl,
        authMethod,
        hasher,
        password,
        salt).shaped

      shaped <> ({
        tuple =>
          User(
            id = tuple._1,
            identityId = IdentityId(tuple._2, tuple._3),
            firstName = tuple._4,
            lastName = tuple._5,
            fullName = tuple._6,
            email = tuple._7,
            avatarUrl = tuple._8,
            authMethod = tuple._9,
            passwordInfo = (tuple._10, tuple._11, tuple._12) match {
              case (Some(hasher), Some(password), salt) => Some(PasswordInfo(hasher, password, salt))
              case _ => None
            })
      },
      {
        (user: User) =>
          Option((
            user.id,
            user.identityId.userId,
            user.identityId.providerId,
            user.firstName,
            user.lastName,
            user.fullName,
            user.email,
            user.avatarUrl,
            user.authMethod,
            user.passwordInfo.map(_.hasher),
            user.passwordInfo.map(_.password),
            user.passwordInfo.flatMap(_.salt)))
      })
    }
  }
  val users = TableQuery[Users]

  val ds: DataSource = DB.getDataSource()

  def findByIdentityId(id: IdentityId): Option[User] = Database.forDataSource(ds) withSession { implicit session =>
    val q = for {
      u <- users
      if u.userId === id.userId
      if u.providerId === id.providerId
    } yield (u)
    q.list.headOption
  }

  def findByEmailAndProvider(email: String, providerId: String): Option[User] = Database.forDataSource(ds) withSession { implicit session =>
    val q = for {
      u <- users
      if u.email === Option(email)
      if u.providerId === providerId
    } yield (u)
    q.list.headOption
  }

  def save(user: Identity): User = save(User.fromIdentity(user))

  def save(user: User) = Database.forDataSource(ds) withSession { implicit session =>
    findByIdentityId(user.identityId) match {
      case None =>
        val id = (users returning users.map(_.id)).insert(user)
        user.copy(id = Option(id))
      case Some(existingUser) =>
        val q = for {
          u <- users
          if u.id is existingUser.id
        } yield u

        val updatedUser = user.copy(id = existingUser.id)
        q.update(updatedUser)
        updatedUser
    }
  }
}

object User {
  def fromIdentity(i: Identity): User = User(None, i.identityId, i.firstName, i.lastName, i.fullName,
    i.email, i.avatarUrl, i.authMethod, None, None, i.passwordInfo)
}

case class User(
  id: Option[Long],
  identityId: IdentityId,
  firstName: String,
  lastName: String,
  fullName: String,
  email: Option[String],
  avatarUrl: Option[String],
  authMethod: AuthenticationMethod,
  oAuth1Info: Option[OAuth1Info] = None,
  oAuth2Info: Option[OAuth2Info] = None,
  passwordInfo: Option[PasswordInfo]) extends Identity
