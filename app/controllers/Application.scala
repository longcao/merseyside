package controllers

import play.api.mvc._
import play.twirl.api.Html

class Application extends Controller {

  def about = Action {
    val about: Html = views.html.about()
    Ok(views.html.master(about))
  }

}