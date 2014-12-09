package controllers

import org.markdown4j.Markdown4jProcessor

import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Play.current
import play.twirl.api.Html

object ResumeController extends Controller {

  lazy val resumeFile: java.io.File = Play.getFile("public/resume/resume.md")
  lazy val processor: Markdown4jProcessor = new Markdown4jProcessor()
  lazy val md: String = processor.process(resumeFile)

  def resume = Action {
    Ok(views.html.resume(md))
  }

}

