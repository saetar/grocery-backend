package com.gh.helper.dao

import com.gh.helper.config.Configuration
import com.gh.helper.domain._
import java.sql._
import scala.Some
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
import scala.slick.driver.MySQLDriver.simple._
import slick.jdbc.meta.MTable
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException


class UserDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = "jdbc:mysql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  // create tables if not exist
  db.withSession {
    if (MTable.getTables("users").list().isEmpty) {
      Users.ddl.create
    }
  }

  /**
   * Saves customer entity into database.
   *
   * @param customer customer entity to
   * @return saved customer entity
   */
  def create(user: User): Either[Failure, User] = {
    try {
      val createDate = db.withSession {
        Users insert user.copy(createDate = Some(new java.util.Date))
      }
      Right(user.copy(createDate = Some(new java.util.Date)))
    } catch {
      case e: MySQLIntegrityConstraintViolationException => {
        update(user.fbId, user)
      }
      case e: SQLException => {        
        Left(databaseError(e))
      }
    }
  }

  /**
   * Updates customer entity with specified one.
   *
   * @param id       id of the customer to update.
   * @param customer updated customer entity
   * @return updated customer entity
   */
  def update(fbId: String, user: User): Either[Failure, User] = {
    try
      db.withSession {
        Users.where(_.fbId === fbId) update user.copy(fbId = fbId) match {
          case 0 => Left(notFoundError(fbId))
          case _ => Right(user.copy(fbId = fbId))
        }
      }
    catch {
      case e: SQLException => {
        System.out.println(e)
        Left(databaseError(e))
      }
    }
  }

  /**
   * Deletes customer from database.
   *
   * @param id id of the customer to delete
   * @return deleted customer entity
   */
  def delete(id: String): Either[Failure, User] = {
    try {
      db.withTransaction {
        val query = Users.where(_.fbId === id)
        val users = query.run.asInstanceOf[List[User]]
        users.size match {
          case 0 =>
            Left(notFoundError(id))
          case _ => {
            query.delete
            Right(users.head)
          }
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
  def get(id: String): Either[Failure, User] = {
    try {
      db.withSession {
        Users.findById(id).firstOption match {
          case Some(user: User) =>
            Right(user)
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

  protected def notFoundError(fbId: String) = 
    Failure("User with fbId=%s does not exist".format(fbId), FailureType.NotFound)
}