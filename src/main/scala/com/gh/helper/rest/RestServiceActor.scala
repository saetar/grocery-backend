package com.gh.helper.rest

import akka.actor.Actor
import akka.event.slf4j.SLF4JLogging
import com.gh.helper.dao.UserDAO
import com.gh.helper.dao.GroceryListDAO
import com.gh.helper.dao.ItemDAO
import com.gh.helper.domain._
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import net.liftweb.json.Serialization._
import net.liftweb.json.{DateFormat, Formats}
import scala.Some
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing._

/**
 * REST Service actor.
 */
class RestServiceActor extends Actor with RestService {

  implicit def actorRefFactory = context

  def receive = runRoute(rest)
}

/**
 * REST Service
 */
trait RestService extends HttpService with SLF4JLogging {

  val userService = new UserDAO
  val groceryListService = new GroceryListDAO
  val itemService = new ItemDAO

  implicit val executionContext = actorRefFactory.dispatcher

  implicit val liftJsonFormats = new Formats {
    val dateFormat = new DateFormat {
      val sdf = new SimpleDateFormat("yyyy-MM-dd")

      def parse(s: String): Option[Date] = try {
        Some(sdf.parse(s))
      } catch {
        case e: Exception => None
      }

      def format(d: Date): String = sdf.format(d)
    }
  }

  implicit val string2Date = new FromStringDeserializer[Date] {
    def apply(value: String) = {
      val sdf = new SimpleDateFormat("yyyy-MM-dd")
      try Right(sdf.parse(value))
      catch {
        case e: ParseException => {
          Left(MalformedContent("'%s' is not a valid Date value" format (value), e))
        }
      }
    }
  }

  implicit val customRejectionHandler = RejectionHandler {
    case rejections => mapHttpResponse {
      response =>
        response.withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),
          write(Map("error" -> response.entity.asString))))
    } {
      RejectionHandler.Default(rejections)
    }
  }

  val rest = respondWithMediaType(MediaTypes.`application/json`) {
      path("grocerylist") {
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug("Retrieving all grocerylists")
              groceryListService.getAll
            }
        } ~
        post {
          entity(Unmarshaller(MediaTypes.`application/json`) {
            case httpEntity: HttpEntity =>
              read[GroceryList](httpEntity.asString(HttpCharsets.`UTF-8`))
          }) {
            groceryList: GroceryList =>
              ctx: RequestContext =>
                handleRequest(ctx, StatusCodes.Created) {
                  log.debug("Creating groceryList: %s".format(groceryList))
                  groceryListService.create(groceryList)
                }
          }
        } ~
        get {
          parameters('title.as[String] ?, 'store.as[String] ?, 'details.as[String] ?).as(GroceryListSearchParameters) {
            searchParameters: GroceryListSearchParameters => {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Searching for customers with parameters: %s".format(searchParameters))
                  groceryListService.search(searchParameters)
                }
            }
          }
        }
      } ~
      path("grocerylist" / LongNumber) {
        groceryListId =>
          get {
            ctx: RequestContext =>
              handleRequest(ctx) {
                log.debug("Searching for grocerylist: %s" format groceryListId)
                groceryListService.get(groceryListId)
              }
          } ~
          delete {
            ctx: RequestContext =>
              handleRequest(ctx) {
                log.debug("Deleting grocerylist: %s" format groceryListId)
                groceryListService.delete(groceryListId)
              }
          } ~
          post {
            entity(Unmarshaller(MediaTypes.`application/json`) {
              case httpEntity: HttpEntity =>
                read[Item](httpEntity.asString(HttpCharsets.`UTF-8`))
            }) {
              item: Item =>
                ctx: RequestContext =>
                  handleRequest(ctx, StatusCodes.Created) {
                    log.debug("Creating groceryList: %s".format(item))
                    itemService.create(groceryListId, item)
                  }
            }
          }
      } ~
      path("user") {
        post {
          entity(Unmarshaller(MediaTypes.`application/json`) {
            case httpEntity: HttpEntity =>
              read[User](httpEntity.asString(HttpCharsets.`UTF-8`))
          }) {
            user: User =>
              ctx: RequestContext =>
                handleRequest(ctx, StatusCodes.Created) {
                  log.debug("Post user")
                  userService.create(user)
                }
          }
        }
      } ~
      path("user" / LongNumber / "grocerylist") {
        fbId =>
          get {
            ctx: RequestContext =>
              handleRequest(ctx) {
                log.debug("Getting user with fbId: %s" format fbId)
                groceryListService.getUserLists(fbId.toString)
              }
          }
      } 
}

  /**
   * Handles an incoming request and create valid response for it.
   *
   * @param ctx         request context
   * @param successCode HTTP Status code for success
   * @param action      action to perform
   */
  protected def handleRequest(ctx: RequestContext, successCode: StatusCode = StatusCodes.OK)(action: => Either[Failure, _]) {
    action match {
      case Right(result: Object) =>
        ctx.complete(successCode, write(result))
      case Left(error: Failure) =>
        ctx.complete(error.getStatusCode, net.liftweb.json.Serialization.write(Map("error" -> error.message)))
      case _ =>
        ctx.complete(StatusCodes.InternalServerError)
    }
  }
}