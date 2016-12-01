package com.gh.helper.domain

import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Customer entity.
 *
 * @param id        unique id
 * @param firstName first name
 * @param lastName  last name
 * @param fbToken   facebook access token
 */
case class User(fbId: String, email: String, firstName: String, lastName: String, curToken: String, createDate: Option[java.sql.Timestamp])

/**
 * Mapped customers table object.
 */
class Users(tag: Tag) extends Table[User](tag, "users") {

  def fbId = column[String]("fbId", O.PrimaryKey, O.Length(64))

  def email = column[String]("email", O.Length(64))

  def firstName = column[String]("firstName", O.Length(64))

  def lastName = column[String]("lastName", O.Length(64))

  def curToken = column[String]("curToken", O.Length(300))

  def createDate = column[java.sql.Timestamp]("createDate")

  def * = (fbId, email, firstName, lastName, curToken, createDate.?) <>(User.tupled, User.unapply)

  def uniqueFbid = index("unique_fbId", fbId, unique = true)

  def uniqueEmail = index("unique_email", email, unique = true)
}

object UserT {
  val users = TableQuery[Users]
}
