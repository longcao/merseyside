package utils

import org.hashids._

import play.api.Play

object Hasher {
  import play.api.Play.current

  private val salt: String = Play.configuration
    .getString("hashids.salt")
    .getOrElse(throw new Exception("missing hashids.salt config value"))

  private val hasher: Hashids = Hashids(salt)

  def generateNewSlug: String = hasher.encode(math.abs(new java.util.Random().nextLong()))
}
