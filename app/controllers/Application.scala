package controllers

import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.templates.Html

object Application extends Controller {

  def home = Action {
    Ok(views.html.master(Html.empty))
  }

  def about = Action {
    val about: Html = views.html.about()
    Ok(views.html.master(about))
  }

}