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
case class User(fbId: String, firstName: String, lastName: String, createDate: Option[java.sql.Timestamp])

/**
 * Mapped customers table object.
 */
class Users(tag: Tag) extends Table[User](tag, "users") {

  def fbId = column[String]("fbId", O.PrimaryKey, O.Length(64))

  def firstName = column[String]("firstName", O.Length(64))

  def lastName = column[String]("lastName", O.Length(64))

  def createDate = column[java.sql.Timestamp]("createDate")

  def * = (fbId, firstName, lastName, createDate.?) <>(User.tupled, User.unapply)

  def uniqueFbid = index("unique_fbId", fbId, unique = true)

  // val findById = for {
  //   fbId <- Parameters[String]
  //   c <- this if c.fbId is fbId
  // } yield c
}

object UserT {
  val users = TableQuery[Users]
}
