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

class UserDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = "jdbc:mysql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  private val users = TableQuery[Users]
  private val friends = TableQuery[Friends]
  // private val facebookService = new FacebookDAO
  // create tables if not exist
  Await.result(db.run(DBIO.seq(
    MTable.getTables map (tables => {
      if (!tables.exists(_.name.name == users.baseTableRow.tableName))
        Await.result(db.run(users.schema.create), Duration.Inf)
      if (!tables.exists(_.name.name == friends.baseTableRow.tableName))
        Await.result(db.run(friends.schema.create), Duration.Inf)
      })
    )), Duration.Inf)

  /**
   * Saves customer entity into database.
   *
   * @param customer customer entity to
   * @return saved customer entity
   */
  def create(user: User): Either[Failure, User] = {
    try {
      var query = users += user
      Await.result(db.run(query), Duration.Inf) match {
        case 0 => {
          Left(databaseError("Error: could not insert user"))
        }
        case _ => {
          Right(user.copy(createDate = Some(new Timestamp((new Date).getTime))))
        }
      }

    } catch {
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
    try { 
      val query = users.filter { _.fbId === fbId } update user.copy(fbId = fbId)
      Await.result(db.run(query), Duration.Inf) match {
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

  def login(fbId: String, fbToken: String): Either[Failure, User] = {
    get(fbId)
    // try {
    //   facebookService.authorizeUser(fbToken) match {
    //     case Some(m: Map[String, Any]) => {
    //       if (m.get("id") == fbId) {
    //         get(fbId) match {
    //           case Right(x) => update(fbId, x.copy(curFBToken = fbToken))
    //           case Left(x) => Left(userCredentialsExpired(fbId))
    //         }
    //       }
    //       else {
    //         Left(userCredentialsExpired(fbId))
    //       }
    //     }
    //     case _ => Left(userCredentialsExpired(fbId))
    //   }
    // }
    // catch {
    //   case e: SQLException => {
    //     System.out.println(e)
    //     Left(databaseError(e))
    //   }
    // }
  }

  /**
   * Deletes customer from database.
   *
   * @param id id of the customer to delete
   * @return deleted customer entity
   */
  def delete(id: String): Either[Failure, String] = {
    try {
      val query = users.filter { _.fbId === id }.delete
      Await.result(db.run(query), Duration.Inf) match {
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
  def get(id: String): Either[Failure, User] = {
    try {
      Await.result(db.run(users.filter { _.fbId === id }.result), Duration.Inf) match {
        case users: Seq[User] =>
          Right(users.head)
        case _ =>
          Left(notFoundError(id))
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * creates access token using timestamp of lastupdate, email and fbtoken
   */
  def makeToken(user: User): (java.util.UUID, String) = {
    var uuid = java.util.UUID.randomUUID
    (uuid, s"${user.createDate}:${user.curFBToken}:${user.email}:${uuid}")
  }

  def getFriends(userId: String): Either[Failure, Seq[User]] = {
    val sideOne = (for {
        (f, u) <- friends.filter(_.userId === userId) join users on (_.otherUserId === _.fbId)
      } yield u)
    val sideTwo = (for {
        (f, u) <- friends.filter(_.userId === userId) join users on (_.userId === _.fbId)
      } yield u)
    val union = sideOne union sideTwo
    try {
      Await.result(db.run(union.result), Duration.Inf).toList match {
        case users: Seq[User] => 
          Right(users)
        case _ => 
          Left(notFoundError(userId))
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

  protected def databaseError(message: String) = 
    Failure("%s could not be inserted".format(message), FailureType.DatabaseFailure)

  protected def userCredentialsExpired(fbId: String) =
    Failure(s"User with fbId $fbId has tried to log in with expired credentials", FailureType.NotFound)
}