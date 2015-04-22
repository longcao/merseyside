package model

import org.joda.time.LocalDate

case class Post(
  title: String,
  date: LocalDate,
  permalink: String,
  content: String)
