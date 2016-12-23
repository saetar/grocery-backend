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
case class User(fbId: String, email: String, firstName: String, lastName: String, 
  curFBToken: String, createDate: Option[java.sql.Timestamp])

/**
 * Mapped customers table object.
 */
class Users(tag: Tag) extends Table[User](tag, "users") {

  def fbId = column[String]("fbId", O.PrimaryKey, O.Length(64))

  def email = column[String]("email", O.Length(64))

  def firstName = column[String]("firstName", O.Length(64))

  def lastName = column[String]("lastName", O.Length(64))

  def curFBToken = column[String]("curToken", O.Length(300))

  def createDate = column[java.sql.Timestamp]("createDate")

  def * = (fbId, email, firstName, lastName, curFBToken, createDate.?) <>(User.tupled, User.unapply)

  def uniqueFbid = index("unique_fbId", fbId, unique = true)

  def uniqueEmail = index("unique_email", email, unique = true)
}


case class Friend(id: Option[Long], userId: String, otherUserId: String, isRemoved: Boolean = false)

class Friends(tag: Tag) extends Table[Friend](tag, "friends") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[String]("userId", O.Length(64))

  def otherUserId = column[String]("otherUserId", O.Length(64))

  def isRemoved = column[Boolean]("isRemoved")

  def * = (id.?, userId, otherUserId, isRemoved) <>(Friend.tupled, Friend.unapply)

  def user = foreignKey("user_friend1_FK", userId, UserT.users)(_.fbId, onUpdate=ForeignKeyAction.Restrict)

  def user2 = foreignKey("user_friend2_FK", otherUserId, UserT.users)(_.fbId, onUpdate=ForeignKeyAction.Restrict)

  def uniqueFriends = index("unique_friends", (userId, otherUserId), unique = true)

}

object UserT {
  val users = TableQuery[Users]
}
