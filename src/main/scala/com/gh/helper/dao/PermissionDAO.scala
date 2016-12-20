package com.gh.helper.dao

import com.gh.helper.config.Configuration
import com.gh.helper.domain._
import java.sql._
import java.sql.Timestamp
import java.util.Date
import scala.Some
import slick.jdbc.JdbcBackend.Database
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

class PermissionDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = "jdbc:mysql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  private val permissions = TableQuery[Permissions]
  private val securities = TableQuery[Securities]

  // create tables if not exist
  Await.result(db.run(DBIO.seq(
  MTable.getTables map (tables => {
    if (!tables.exists(_.name.name == permissions.baseTableRow.tableName))
      Await.result(db.run(permissions.schema.create), Duration.Inf)
    if (!tables.exists(_.name.name == securities.baseTableRow.tableName))
      Await.result(db.run(securities.schema.create), Duration.Inf)
    })
  )), Duration.Inf)


  /**
   * Saves customer entity into database.
   *
   * @param customer customer entity to
   * @return saved customer entity
   */
  def create(permission: Permission): Either[Failure, Permission] = {
    try {
      var query = permissions += permission
      Await.result(db.run(query), Duration.Inf) match {
        case 0 => {
          Left(userCredentialsInvalid("Error: could not insert user"))
        }
        case _ => {
          Right(permission.copy(createDate = Some(new Timestamp((new Date).getTime))))
        }
      }

    } catch {
      case e: SQLException => {        
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
  def delete(id: Long): Either[Failure, Long] = {
    try {
      val query = permissions.filter { _.id === id }.delete
      Await.result(db.run(query), Duration.Inf) match {
        case 0 =>
          Left(userCredentialsInvalid(id.toString))
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
  def get(id: Long): Either[Failure, Permission] = {
    try {
      Await.result(db.run(permissions.filter { _.id === id }.result), Duration.Inf) match {
        case permissions: Seq[Permission] =>
          Right(permissions.head)
        case _ =>
          Left(notFoundError(id))
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }


  def getUserPermissions(userId: String): Either[Failure, Seq[String]] = {
    try {
      val query = permissions.filter(_.userId === userId).result
      Await.result(db.run(query), Duration.Inf).toList match {
        case res: Seq[Permission] => 
          Right(res.map( _.permission.toString ))
        case _ => 
          Left(notFoundError(userId))
      }
    } catch {
      case e: SQLException => 
        Left(databaseError(e))
    }
  }

  def userHasPermission(authorization: String, pt: PermissionType.Value): Boolean = {
    if (pt == PermissionType.Nothing) {
      true
    }
    else {
      val userId = authorization.split(":")(0)
      getUserPermissions(userId) match {
        case seq: Seq[String] => seq.contains(pt.toString)
        case _ => false
      }
    }
  }

  def create(security: Security): Either[Failure, Security] = {
    try {
      var query = securities += security
      Await.result(db.run(query), Duration.Inf) match {
        case 0 => {
          Left(userCredentialsInvalid("Error: could not insert user"))
        }
        case _ => {
          Right(security.copy(createDate = Some(new Timestamp((new Date).getTime))))
        }
      }

    } catch {
      case e: SQLException => {        
        Left(databaseError(e))
      }
    }
  }

  def makeUserSecurity(userId: String, fbToken: String): Either[Failure,Security] = {
    val curToken = s"$userId:$fbToken"
    val security = Security(null, userId, fbToken, curToken, null)
    create(security)
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
  protected def userCredentialsInvalid(userId: String) =
    Failure(s"User with fbId $userId does not have valid credentials", FailureType.NotFound)

  protected def notFoundError(customerId: Long) =
    Failure(s"Customer with id=$customerId does not exist", FailureType.NotFound)

  protected def notFoundError(userId: String) =
    Failure(s"Customer with id=$userId does not exist", FailureType.NotFound)
}