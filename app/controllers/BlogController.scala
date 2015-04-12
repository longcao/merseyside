package controllers

import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import scala.concurrent.Future

object BlogController extends Controller {

  def frontpage = Action.async { request =>
    val frontpage = views.html.blog.frontpage()
    Future.successful(Ok(views.html.master(frontpage)))
  }

}
