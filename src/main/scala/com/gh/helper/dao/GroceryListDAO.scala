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
class GroceryListDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = "jdbc:mysql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  // create tables if not exist
  db.withSession {
    if (MTable.getTables("lists").list().isEmpty) {
      GroceryLists.ddl.create
    }
  }

  /**
   * Saves groceryList entity into database.
   *
   * @param list list entity to
   * @return saved list entity
   */
  def create(list: GroceryList): Either[Failure, GroceryList] = {
    try {
      val id = db.withSession {
        GroceryLists returning GroceryLists.id insert list.copy(createDate = Some(new Date))
      }
      Right(list.copy(id = Some(id), createDate = Some(new Date)))
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
  def update(id: Long, list: GroceryList): Either[Failure, GroceryList] = {
    try
      db.withSession {
        GroceryLists.where(_.id === id) update list.copy(id = Some(id)) match {
          case 0 => Left(notFoundError(id))
          case _ => Right(list.copy(id = Some(id)))
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
  def delete(id: Long): Either[Failure, GroceryList] = {
    try {
      db.withTransaction {
        val query = GroceryLists.where(_.id === id)
        val lists = query.run.asInstanceOf[List[GroceryList]]
        lists.size match {
          case 0 =>
            Left(notFoundError(id))
          case _ => {
            query.delete
            Right(lists.head)
          }
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  def getAll(): Either[Failure, List[GroceryList]] = {
    try {
      db.withSession {
        val query = for {
          groceryList <- GroceryLists 
        } yield groceryList
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
  def get(id: Long): Either[Failure, GroceryList] = {
    try {
      db.withSession {
        GroceryLists.findById(id).firstOption match {
          case Some(groceryList: GroceryList) =>
            Right(groceryList)
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
  def getUserLists(userId: String): Either[Failure, List[GroceryList]] = {
    try {
      db.withSession {
        // val query = GroceryLists.filter(_.userId === userId)
        val query = for {
          groceryList <- GroceryLists if {
            groceryList.userId === userId
          }
        } yield groceryList
        val lists: List[GroceryList] = query.run.toList.sortBy(_.createDate)
        lists.size match {
          case 0 => 
            Left(noListForUser(userId))
          case _ => {
            Right(lists)
          }
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }


  /**
   * Retrieves list of customers with specified parameters from database.
   *
   * @param params search parameters
   * @return list of customers that match given parameters
   */
  def search(params: GroceryListSearchParameters): Either[Failure, List[GroceryList]] = {
    implicit val typeMapper = GroceryLists.dateTypeMapper

    try {
      db.withSession {
        val query = for {
          groceryList <- GroceryLists if {
            Seq(
              params.title.map(groceryList.title is _),
              params.store.map(groceryList.store is _),
              params.details.map(groceryList.details is _)
            ).flatten match {
              case Nil => ConstColumn.TRUE
              case seq => seq.reduce(_ && _)
            }
          }
        } yield groceryList

        Right(query.run.toList)
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


  protected def noListForUser(userId: String) = 
    Failure("User with id=%d does not exist".format(userId), FailureType.NotFound)




}