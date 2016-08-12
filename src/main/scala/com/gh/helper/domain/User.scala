package com.gh.helper.domain

import scala.slick.driver.MySQLDriver.simple._

/**
 * Customer entity.
 *
 * @param id        unique id
 * @param firstName first name
 * @param lastName  last name
 * @param fbToken   facebook access token
 */
case class User(fbId: String, firstName: String, lastName: String, createDate: Option[java.util.Date])

/**
 * Mapped customers table object.
 */
object Users extends Table[User]("users") {

  def fbId = column[String]("fbId", O.PrimaryKey)

  def firstName = column[String]("firstName")

  def lastName = column[String]("lastName")

  def createDate = column[java.util.Date]("createDate")

  def * = fbId ~ firstName ~ lastName ~ createDate.? <>(User, User.unapply _)

  def uniqueFbid = index("unique_fbId", fbId, unique = true)

  implicit val dateTypeMapper = MappedTypeMapper.base[java.util.Date, java.sql.Timestamp](
  {
    ud => new java.sql.Timestamp(ud.getTime)
  }, {
    ts => new java.util.Date(ts.getTime)
  })

  val findById = for {
    fbId <- Parameters[String]
    c <- this if c.fbId is fbId
  } yield c
}