package service

import java.io.File

import model.{ Post, Yaml }

import org.joda.time.LocalDate

import org.markdown4j.Markdown4jProcessor

import play.api.Play
import play.api.Play.current

import scala.io.Source

object PostService {
  private val processor: Markdown4jProcessor = new Markdown4jProcessor()

  val postFileRegex = "^(\\d{4})-(\\d{2})-(\\d{2})-(.*)(\\.[^.]+)$".r

  case class ParsedFileName(year: Int, month: Int, day: Int, slug: String) {
    private def leadingZero(i: Int): String = "%02d".format(i)

    val leadingMonth = leadingZero(month)
    val leadingDay = leadingZero(day)
  }

  /**
   * Constructs a permalink path given the parsed filename,
   * in the form of: /YYYY/MM/DD/slug
   */
  private def parsePermalink(parsed: ParsedFileName): String = {
    s"/${parsed.year}/${parsed.leadingMonth}/${parsed.leadingDay}/${parsed.slug}"
  }

  private def parseDate(parsed: ParsedFileName): LocalDate = {
    val dateString = s"${parsed.year}-${parsed.leadingMonth}-${parsed.leadingDay}"

    LocalDate.parse(dateString)
  }

  /**
   * Separate the front matter string from the rest of the post.
   */
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
          val parsed = ParsedFileName(year.toInt, month.toInt, day.toInt, slug)

          // YAML
          val (frontMatter, rest) = separateFrontMatter(file)
          val yaml = Yaml.parseFrontMatter(frontMatter)

          // Post properties
          val title = yaml.get[String]("title")
          val date = parseDate(parsed)
          val permalink = parsePermalink(parsed)
          val tags = yaml.get[Seq[String]]("tags").getOrElse(Seq.empty)

          val post = Post(
            title = title,
            date = date,
            permalink = permalink,
            tags = tags,
            content = processor.process(rest))

          Some(permalink -> post)
        case _ => None
      }
    }.toMap

  val allPosts: Seq[Post] = posts.values.toSeq

}
