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
class GroceryListDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = "jdbc:mysql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  private val groceryLists = TableQuery[GroceryLists]

   // create tables if not exist
  Await.result(db.run(DBIO.seq(
    MTable.getTables map (tables => {
      if (!tables.exists(_.name.name == groceryLists.baseTableRow.tableName))
        Await.result(db.run(groceryLists.schema.create), Duration.Inf)
    })
  )), Duration.Inf)

  private val itemService = new ItemDAO

  val insertQuery = groceryLists returning groceryLists.map(_.id) into ((list, id) => list.copy(id = Some(id)))

  /**
   * Saves groceryList entity into database.
   *
   * @param list list entity to
   * @return saved list entity
   */
  def create(list: GroceryList): Either[Failure, GroceryList] = {
    try {
      val action = insertQuery += list.copy(isDeleted = Some(false), 
        createDate = Some(new Timestamp((new Date).getTime)))
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
  def update(id: Long, list: GroceryList): Either[Failure, GroceryList] = {
    try {
      val action = groceryLists.filter(_.id === id) update list.copy(id = Some(id))
      val res = Await.result(db.run(action), Duration.Inf)
      res match {
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
  def delete(id: Long): Either[Failure, Int] = {
    try {
      val action = groceryLists.filter(_.id === id).result
      val res = Await.result(db.run(action), Duration.Inf).head
      val del = groceryLists.filter(_.id === id) update res.copy(isDeleted = Some(true))
      val res2 = Await.result(db.run(del), Duration.Inf)
      Right(res2)
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
      var action = groceryLists.filter { _.id === id }.result
      var list = Await.result(db.run(action), Duration.Inf).head
      list match {
        case groceryList: GroceryList =>
          Right(groceryList)
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
   * @param  userId
   * @return list of grocerylists that match given userId
   */
  def getUserLists(userId: String): Either[Failure, List[GroceryList]] = {
    try {
      // val query = groceryLists.filter(_.userId === userId)
      val query = groceryLists.filter(_.userId === userId).filter(!_.isDeleted).result
      val lists = Await.result(db.run(query), Duration.Inf).toList
      lists.size match {
        case 0 =>
          Left(noListForUser(userId))
        case _ => {
          Right(lists)
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
    implicit val dateTypeMapper = MappedColumnType.base[java.util.Date, java.sql.Timestamp](
    {
      ud => new java.sql.Timestamp(ud.getTime)
    }, {
      ts => new java.util.Date(ts.getTime)
    })

    try {
      val query = for {
        groceryList <- groceryLists if {
          Seq(
            params.title.map(groceryList.title === _),
            params.store.map(groceryList.store === _),
            params.details.map(groceryList.details === _)
          ).flatten match {
            // case Nil => Nil
            case seq => seq.reduce(_ && _)
          }
        }
      } yield groceryList
      val res = Await.result(db.run(query.result), Duration.Inf).toList
      Right(res)
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
    Failure(s"Customer with id=$customerId does not exist", FailureType.NotFound)


  protected def noListForUser(userId: String) =
    Failure(s"User with id=$userId does not exist", FailureType.NotFound)

}
