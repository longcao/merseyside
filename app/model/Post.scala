package model

import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDate

object Post {
  implicit val postOrdering: Ordering[Post] = new Ordering[Post] {
    def compare(p1: Post, p2: Post): Int = p2.date.compareTo(p1.date)
  }
  val rfc822format = DateTimeFormat.forPattern("EEE, dd MMM yyyy")
}

case class Post(
  title: Option[String],
  date: LocalDate,
  permalink: String,
  tags: Seq[String],
  content: String) {

  // ex: Tue, 08 Jun 2015 12:00:00 GMT
  def formattedDate: String = s"${Post.rfc822format.print(date)} 12:00:00 GMT"

  // domain name and full path
  def fullPermalink: String = s"http://longcao.org$permalink"
}
