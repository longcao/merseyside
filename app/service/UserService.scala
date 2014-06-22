package service

import repository.{ TokenRepository, UserRepository }

import org.joda.time.DateTime

import play.api.Application

import securesocial.core._
import securesocial.core.providers.Token

class UserService(application: Application) extends UserServicePlugin(application) {
  def find(id: IdentityId): Option[Identity] = UserRepository.findByIdentityId(id)

  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] =
    UserRepository.findByEmailAndProvider(email, providerId)

  def save(user: Identity) = UserRepository.save(user)

  def save(token: Token) = TokenRepository.save(token)

  def findToken(token: String): Option[Token] = TokenRepository.findById(token)

  def deleteToken(uuid: String) = TokenRepository.delete(uuid)

  def deleteExpiredTokens = TokenRepository.deleteExpiredTokens(DateTime.now())
}
