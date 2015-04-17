package service

import java.io.File

import model.Post

import org.markdown4j.Markdown4jProcessor

import play.api.Play
import play.api.Play.current

object PostService {
  private val processor: Markdown4jProcessor = new Markdown4jProcessor()

  val postFileRegex = "^(\\d{4})-(\\d{2})-(\\d{2})-(.*)(\\.[^.]+)$".r

  case class ParsedFileName(year: Int, month: Int, day: Int, slug: String)

  private def createPermalink(parsed: ParsedFileName): String = {
    def leadingZero(i: Int): String = "%02d".format(i)
    val leadingMonth = leadingZero(parsed.month)
    val leadingDay = leadingZero(parsed.day)

    s"/${parsed.year}/${leadingMonth}/${leadingDay}/${parsed.slug}"
  }

  val posts: Map[String, Post] = Play.getFile("_posts")
    .listFiles
    .flatMap { file =>
      file.getName match {
        case postFileRegex(year, month, day, slug, extension) =>
          Some((ParsedFileName(year.toInt, month.toInt, day.toInt, slug), file))
        case _ => None
      }
    }.map { case (parsed, file) =>
      val permalink = createPermalink(parsed)
      val post = Post(
        title = "Placeholder",
        permalink = permalink,
        content = processor.process(file))

      permalink -> post
    }.toMap
}
