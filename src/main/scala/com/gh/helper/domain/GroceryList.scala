package com.gh.helper.domain

import scala.slick.driver.MySQLDriver.simple._

/**
 * Customer entity.
 *
 * @param id        unique id
 * @param title     title
 * @param store     store
 * @param details   details
 */
case class GroceryList(id: Option[Long], userId: String, title: String, store: String, details: String, createDate: Option[java.util.Date])

/**
 * Mapped customers table object.
 */
object GroceryLists extends Table[GroceryList]("lists") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[String]("userId")

  def title = column[String]("title")

  def store = column[String]("store")

  def details = column[String]("details")

  def createDate = column[java.util.Date]("createDate", O.Nullable)

  def * = id.? ~ userId ~ title ~ store ~ details ~ createDate.? <>(GroceryList, GroceryList.unapply _)

  //def supplier = foreignKey("SUP_FK", supID, suppliers)(_.id)

  def user = foreignKey("user_FK", userId, Users)(_.fbId)

  implicit val dateTypeMapper = MappedTypeMapper.base[java.util.Date, java.sql.Timestamp](
  {
    ud => new java.sql.Timestamp(ud.getTime)
  }, {
    ts => new java.util.Date(ts.getTime)
  })

  val findById = for {
    id <- Parameters[Long]
    c <- this if c.id is id
  } yield c

  val findByUserId = for {
    fbId <- Parameters[String]
    c <- this if c.userId is fbId
  } yield c
}