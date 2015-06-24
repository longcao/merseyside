package model

import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone, LocalDate }

object Post {
  implicit val postOrdering: Ordering[Post] = new Ordering[Post] {
    def compare(p1: Post, p2: Post): Int = p2.date.compareTo(p1.date)
  }
  val rfc822format = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z")
  val rfc3339format = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
}

case class Post(
  title: Option[String],
  date: LocalDate,
  permalink: String,
  tags: Seq[String],
  content: String) {

  // default to noon UTC since no timestamp on posts
  val defaultDate: DateTime = date.toDateTimeAtStartOfDay(DateTimeZone.UTC)
    .plusHours(12)

  // ex: Tue, 08 Jun 2015 12:00:00 GMT
  def formattedDateRFC822: String = s"${Post.rfc822format.print(defaultDate)}"

  // ex: 2015-06-08T18:30:02Z
  def formattedDateRFC3339: String = s"${Post.rfc3339format.print(defaultDate)}"

  // domain name and full path
  def fullPermalink: String = s"http://longcao.org$permalink"
}
