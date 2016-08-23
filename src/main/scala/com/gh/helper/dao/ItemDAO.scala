package com.gh.helper.dao

import com.gh.helper.config.Configuration
import com.gh.helper.domain._
import java.sql._
import java.util.Date
import scala.Some
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
import scala.slick.driver.MySQLDriver.simple._
import slick.jdbc.meta.MTable

/**
 * Provides DAL for GroceryList entities for MySQL database.
 */
class ItemDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = "jdbc:mysql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  // create tables if not exist
  db.withSession {
    if (MTable.getTables("items").list().isEmpty) {
      Items.ddl.create
    }
  }

  /**
   * Saves groceryList entity into database.
   *
   * @param list list entity to
   * @return saved list entity
   */
  def create(groceryListId: Long, item: Item): Either[Failure, Item] = {
    try {
      val id = db.withSession {
        Items returning Items.id insert item.copy(listId = Some(groceryListId), createDate = Some(new Date))
      }
      Right(item.copy(id = Some(id), listId = Some(groceryListId), createDate = Some(new Date)))
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
    try
      db.withSession {
        Items.where(_.id === id) update item.copy(id = Some(id)) match {
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
  def delete(id: Long): Either[Failure, Item] = {
    try {
      db.withTransaction {
        val query = GroceryLists.where(_.id === id)
        val items = query.run.asInstanceOf[List[Item]]
        items.size match {
          case 0 =>
            Left(notFoundError(id))
          case _ => {
            query.delete
            Right(items.head)
          }
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  def getAll(): Either[Failure, List[Item]] = {
    try {
      db.withSession {
        val query = for {
          item <- Items 
        } yield item
        val sortedData = query.run.toList.sortBy(- _.createDate.get.getTime)
        Right(sortedData)
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
      db.withSession {
        Items.findById(id).firstOption match {
          case Some(item: Item) =>
            Right(item)
          case _ =>
            Left(notFoundError(id))
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }
  
  /**
   * Retrieves list of grocerylists from a particular user id in the database
   *
   * @param  userId
   * @return list of grocerylists that match given userId
   */
  def getListItems(listId: Long): Either[Failure, List[Item]] = {
    try {
      // System.out.println("Finding items with listID")
      db.withSession {
        val query = for {
          item <- Items if {
            item.listId === listId
          }
        } yield item
        query.run.toList.sortBy(_.createDate) match {
          case items: List[Item] => 
            System.out.println("Got here (147) " + items)
            Right(items)
          case _ => {
            Left(noItemsInList(listId))
          }
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


  protected def noItemsInList(listId: Long) = 
    Failure("Items with list id=%d does not exist".format(listId), FailureType.NotFound)




}