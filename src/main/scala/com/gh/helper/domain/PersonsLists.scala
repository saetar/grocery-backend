package com.gh.helper.domain

// import slick.driver.MySQLDriver.simple._
import slick.model.ForeignKeyAction
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import java.sql.Timestamp


case class PersonList(userId: String, listId: Long, isOwner: Boolean)

class PersonLists(tag: Tag) extends Table[PersonList](tag, "userlist") {

  def userId = column[String]("userId", O.Length(64))

  def listId = column[Long]("listId")

  def isOwner = column[Boolean]("isOwner")

  def * = (userId, listId, isOwner) <> ((PersonList.apply _).tupled, PersonList.unapply)

  def user = foreignKey("user_FK", userId, TableQuery[Users])(_.fbId)

  def list = foreignKey("list_FK", listId, TableQuery[GroceryLists])(_.id)

  def uniqueConst = index("unique_idx", (userId, listId), unique = true)

}
