package service

import java.io.File

import model.Post

import org.markdown4j.Markdown4jProcessor

import play.api.Play
import play.api.Play.current

object PostService {
  private val processor: Markdown4jProcessor = new Markdown4jProcessor()

  val posts = Play.getFile("_posts")
    .listFiles
    .toList
    .map { f =>
      Post(
        title = "Placeholder",
        permalink = "/2014/05/12/placeholder-title",
        content = processor.process(f))
    }
}
