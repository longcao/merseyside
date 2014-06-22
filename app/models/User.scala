package models

import securesocial.core._

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
