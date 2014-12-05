package collections

import models.Post

import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.MongoController

import reactivemongo.api._
import reactivemongo.api.indexes._

trait PostCollection extends MongoController {
  self: Controller =>

  lazy val postCollection: JSONCollection = db.collection[JSONCollection]("posts")

  postCollection.indexesManager.ensure(Index(
    Seq(
      "slug" -> IndexType.Ascending)))

}
