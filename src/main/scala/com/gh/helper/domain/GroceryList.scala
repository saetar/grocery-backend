package com.gh.helper.domain

// import slick.driver.MySQLDriver.simple._
import slick.model.ForeignKeyAction
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import java.sql.Timestamp

/**
 * Customer entity.
 *
 * @param id        unique id
 * @param title     title
 * @param store     store
 * @param details   details
 */
case class GroceryList(id: Option[Long], userId: String, title: String, store: String, 
	details: String, isDeleted: Option[Boolean], createDate: Option[Timestamp])

/**
 * Mapped customers table object.
 */
class GroceryLists(tag: Tag) extends Table[GroceryList](tag, "lists") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[String]("userId", O.Length(64))

  def title = column[String]("title", O.Length(64))

  def store = column[String]("store", O.Length(64))

  def details = column[String]("details", O.Length(64))

  def isDeleted = column[Boolean]("isDeleted", O.Default(false))

  def createDate = column[java.sql.Timestamp]("createDate")

  def * = (id.?, userId, title, store, details, isDeleted.?, createDate.?) <> ((GroceryList.apply _).tupled, GroceryList.unapply)

  def user = foreignKey("user_FK", userId, TableQuery[Users])(_.fbId, onUpdate=ForeignKeyAction.Restrict)
}
