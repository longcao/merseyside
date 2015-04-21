package service

import java.io.File

import model.{ Post, Yaml }

import org.markdown4j.Markdown4jProcessor

import play.api.Play
import play.api.Play.current

import scala.io.Source

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

  private def separateFrontMatter(file: File): (String, String) = {
    val lines = Source.fromFile(file)
      .getLines
      .map(_.trim)
      .dropWhile(_.isEmpty)

    val first = if (lines.hasNext) lines.next else ""

    first match {
      case "---" =>
        val frontMatter = lines.takeWhile(_ != "---").mkString("\n")
        val rest = lines.mkString("\n")
        (frontMatter, rest)
      case _ => ("", lines.mkString("\n"))
    }
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

      val (frontMatter, rest) = separateFrontMatter(file)
      val yaml = Yaml.parseFrontMatter(frontMatter)

      // TODO: parse post front matter here
      val post = Post(
        title = "Placeholder",
        permalink = permalink,
        content = processor.process(rest))

      permalink -> post
    }.toMap
}
