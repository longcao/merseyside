package controllers

import org.markdown4j.Markdown4jProcessor

import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Play.current
import play.twirl.api.Html

object Application extends Controller {

  def about = Action {
    val about: Html = views.html.about()
    Ok(views.html.master(about))
  }

  def resume = Action {
    val resume: java.io.File = Play.getFile("public/resume/resume.md")
    val processor: Markdown4jProcessor = new Markdown4jProcessor()
    val md: String = processor.process(resume)
    Ok(views.html.resume(md))
  }

}