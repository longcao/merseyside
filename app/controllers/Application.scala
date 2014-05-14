package controllers

import play.api._
import play.api.mvc._
import play.api.templates.Html

object Application extends Controller {

  def home = Action {
    val content: Html = views.html.content()
    Ok(views.html.master(content))
  }

}