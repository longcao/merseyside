package service

import repository.AuthenticatorRepository

import play.api.Application

import securesocial.core.{ Authenticator, AuthenticatorStore }

class MySQLAuthenticatorStore(application: Application) extends AuthenticatorStore(application) {
  def save(authenticator: Authenticator): Either[Error, Unit] = {
    AuthenticatorRepository.save(authenticator)
    Right(())
  }

  def find(id: String): Either[Error, Option[Authenticator]] = {
    Right(AuthenticatorRepository.findById(id))
  }

  def delete(id: String): Either[Error, Unit] = {
    AuthenticatorRepository.delete(id)
    Right(())
  }
}
