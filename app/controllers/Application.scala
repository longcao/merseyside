package controllers

import play.api.mvc._
import play.twirl.api.Html

object Application extends Controller {

  def about = Action {
    val about: Html = views.html.about()
    Ok(views.html.master(about))
  }

}