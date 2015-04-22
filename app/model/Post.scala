package model

import org.joda.time.LocalDate

object Post {
  implicit val postOrdering: Ordering[Post] = new Ordering[Post] {
    def compare(p1: Post, p2: Post): Int = p2.date.compareTo(p1.date)
  }
}

case class Post(
  title: String,
  date: LocalDate,
  permalink: String,
  content: String)
