import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.Future

object Global extends GlobalSettings {

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    Future.successful {
      NotFound(views.html.master(views.html.notfound()))
    }
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    Future.successful {
      InternalServerError(views.html.master(views.html.ise()))
    }
  }
}
