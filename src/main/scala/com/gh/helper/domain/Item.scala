package com.gh.helper.domain

import scala.slick.driver.MySQLDriver.simple._

/**
 * Item entity.
 *
 * @param id        unique id
 * @param listId    FK to list
 * @param name      name of item
 * @param price     price of item
 */
case class Item(id: Option[Long], listId: Option[Long], name: String, price: Double, category: Option[String], createDate: Option[java.util.Date])

/**
 * Mapped customers table object.
 */
object Items extends Table[Item]("items") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def listId = column[Long]("listId", O.Nullable)

  def name = column[String]("name")

  def price = column[Double]("price")

  def category = column[String]("category", O.Nullable) 

  def createDate = column[java.util.Date]("createDate")

  def * = id.? ~ listId.? ~ name ~ price ~ category.? ~ createDate.? <> (Item, Item.unapply _)

  // def list = foreignKey("List_FK", listId, GroceryLists)(_.id)

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
}