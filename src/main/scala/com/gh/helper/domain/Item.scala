package com.gh.helper.domain

import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import java.sql.Timestamp
/**
 * Item entity.
 *
 * @param id        unique id
 * @param listId    FK to list
 * @param name      name of item
 * @param price     price of item
 */
case class Item(id: Option[Long], listId: Option[Long], name: String, price: Double, category: Option[String], createDate: Option[Timestamp])

/**
 * Mapped customers table object.
 */
class Items(tag: Tag) extends Table[Item](tag, "items") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def listId = column[Long]("listId")

  def name = column[String]("name", O.Length(64))

  def price = column[Double]("price")

  def category = column[String]("category", O.Length(64)) 

  def createDate = column[Timestamp]("createDate")

  def * = (id.?, listId.?, name, price, category.?, createDate.?) <> (Item.tupled, Item.unapply _)

  // def list = foreignKey("List_FK", listId, GroceryLists)(_.id)

}