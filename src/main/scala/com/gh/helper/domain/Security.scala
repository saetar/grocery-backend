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
case class Security(id: Option[Long], userId: String, fbToken: String, userToken: String, createDate: Option[Timestamp])


class Securities(tag: Tag) extends Table[Security](tag, "security") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[String]("userId", O.Length(64))

  def fbToken = column[String]("fbToken", O.Length(300))

  def userToken = column[String]("curToken", O.Length(300))

  def createDate = column[java.sql.Timestamp]("createDate")

  def * = (id.?, userId, fbToken, userToken, createDate.?) <> ((Security.apply _).tupled, Security.unapply)

  def user = foreignKey("user_security_FK", userId, TableQuery[Users])(_.fbId, onUpdate=ForeignKeyAction.Restrict)
}
