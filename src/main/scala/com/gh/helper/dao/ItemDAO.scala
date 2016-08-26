package com.gh.helper.dao

import com.gh.helper.config.Configuration
import com.gh.helper.domain._
import java.sql._
import java.util.Date
import java.sql.Timestamp
import scala.Some
import slick.jdbc.JdbcBackend.Database
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

/**
 * Provides DAL for GroceryList entities for MySQL database.
 */
class ItemDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = "jdbc:mysql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  private val items = TableQuery[Items]

   // create tables if not exist
  try {
    Await.result(db.run(DBIO.seq(
      MTable.getTables map (tables => {
        if (!tables.exists(_.name.name == items.baseTableRow.tableName)) {
          Await.result(db.run(items.schema.create), Duration.Inf)
        }
      })
    )), Duration.Inf)
  }

  val insertQuery = items returning items.map(_.id) into ((item, id) => item.copy(id = Some(id)))

  /**
   * Saves groceryList entity into database.
   *
   * @param list list entity to
   * @return saved list entity
   */
  def create(groceryListId: Long, item: Item): Either[Failure, Item] = {
    try {
      val action = insertQuery += item.copy(createDate = Some(new Timestamp((new Date).getTime)))
      val res = Await.result(db.run(action),Duration.Inf)
      Right(res)      
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Updates groceryList entity with specified one.
   *
   * @param id       id of the grocerList to update.
   * @param grocerList updated groceryList entity
   * @return updated grocerList entity
   */
  def update(id: Long, item: Item): Either[Failure, Item] = {
    try {
      val query = items.filter { _.id === id } update item.copy(id = Some(id))
      Await.result(db.run(query), Duration.Inf) match {
        case 0 => Left(notFoundError(id))
        case _ => Right(item.copy(id = Some(id)))
      }
    }
    catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Deletes customer from database.
   *
   * @param id id of the customer to delete
   * @return deleted customer entity
   */
  def delete(id: Long): Either[Failure, Long] = {
    try {
      val query = items.filter(_.id === id).delete
      val res = Await.result(db.run(query), Duration.Inf)
      res match {
        case 0 =>
          Left(notFoundError(id))
        case _ => {
          Right(id)
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Retrieves specific customer from database.
   *
   * @param id id of the customer to retrieve
   * @return customer entity with specified id
   */
  def get(id: Long): Either[Failure, Item] = {
    try {
      val query = items.filter{ _.id === id }.result
      Await.result(db.run(query), Duration.Inf) match {
        case item: Item =>
          Right(item)
        case _ =>
          Left(notFoundError(id))
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }
  
  /**
   * Retrieves list of grocerylists from a particular user id in the database
   *
   * @param  listId
   * @return list of items that match given listId
   */
  def getListItems(listId: Long): Either[Failure, Seq[Item]] = {
    try {
      val query = items.filter{ _.listId === listId }.result
      val res = Await.result(db.run(query), Duration.Inf).toList
      res.length match {
        case 0 => 
          Left(noitemsInList(listId))
        case _ => {
          Right(res)
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Produce database error description.
   *
   * @param e SQL Exception
   * @return database error description
   */
  protected def databaseError(e: SQLException) =
    Failure("%d: %s".format(e.getErrorCode, e.getMessage), FailureType.DatabaseFailure)

  /**
   * Produce customer not found error description.
   *
   * @param customerId id of the customer
   * @return not found error description
   */
  protected def notFoundError(customerId: Long) =
    Failure("Customer with id=%d does not exist".format(customerId), FailureType.NotFound)


  protected def noitemsInList(listId: Long) = 
    Failure("items with list id=%d does not exist".format(listId), FailureType.NotFound)




}