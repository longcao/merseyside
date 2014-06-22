package repository

import javax.sql.DataSource

import play.api.db.DB
import play.api.Play.current

trait Repository {
  val ds: DataSource = DB.getDataSource()
}
