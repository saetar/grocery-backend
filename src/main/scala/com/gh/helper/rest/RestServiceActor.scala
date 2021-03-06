package com.gh.helper.rest

import akka.actor.Actor
import akka.event.slf4j.SLF4JLogging
import com.gh.helper.dao._
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
  val permissionService = new PermissionDAO

  implicit val executionContext = actorRefFactory.dispatcher

  implicit val liftJsonFormats = new Formats {
    val dateFormat = new DateFormat {
      val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

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
      val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
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
      post {
        entity(Unmarshaller(MediaTypes.`application/json`) {
          case httpEntity: HttpEntity =>
            read[GroceryList](httpEntity.asString(HttpCharsets.`UTF-8`))
        }) {
          groceryList: GroceryList =>
            ctx: RequestContext =>
              handleRequest(ctx, successCode = StatusCodes.Created) {
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
                handleRequest(ctx, successCode = StatusCodes.Created) {
                  log.debug("Creating groceryList: %s".format(item))
                  itemService.create(groceryListId, item)
                }
          }
        }        
    } ~
    path("grocerylist" / LongNumber / PathElement) {
      (groceryListId, userId) => 
        put {
          ctx: RequestContext =>
            // println(authorization)
            handleRequest(ctx) {
              log.debug(s"Adding user $userId to list $groceryListId")
              groceryListService.addToListUser(groceryListId, userId)
            }
        }
    } ~
    path("grocerylist" / LongNumber / "items") {
      groceryListId =>
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug("Getting items for grocerylist: %s" format groceryListId)
              itemService.getListItems(groceryListId)
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
              handleRequest(ctx, successCode = StatusCodes.Created) {
                log.debug("Post user")
                userService.create(user)
              }
        }
      }
    } ~
    path("login" / PathElement / PathElement) {
      (fbId, fbToken) => 
        get {
          ctx: RequestContext => 
            handleRequest(ctx, PermissionType.Nothing) {
              log.debug(s"Logging in user with fbId $fbId")
              permissionService.makeUserSecurity(fbId, fbToken)
            }
        }
    } ~
    path("user" / PathElement) {
      fbId =>
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug("Getting user with fbId: %s" format fbId)
              userService.get(fbId)
            }
        }          
    } ~
    path("user" / PathElement/ "permissions") {
      fbId => 
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug(s"Getting permissions for user $fbId")
              permissionService.getUserPermissions(fbId)
            }
        }
    } ~
    path("user" / PathElement / "grocerylist") {
      fbId =>
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug("Getting user with fbId: %s" format fbId)
              groceryListService.getUserLists(fbId)
            }
        }
    } ~ 
    path("user" / PathElement / "friends") {
      fbId => 
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug(s"Getting friends for user with id $fbId")
              userService.getFriends(fbId)
            }
        }
    } ~
    path("items" / LongNumber) {
      itemId => 
        delete {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug(s"Deleting item with id $itemId")
              itemService.delete(itemId)
            }
        }
    }
  }

  protected def unauthorizedAction(userId: String) = 
    Failure(s"User with userId $userId does not have permission to access this resource", FailureType.BadRequest)  

  /**
   * Handles an incoming request and create valid response for it.
   *
   * @param ctx         request context
   * @param successCode HTTP Status code for success
   * @param action      action to perform
   */
  protected def handleRequest(ctx: RequestContext, permission: PermissionType.Value = PermissionType.Normal, successCode: StatusCode = StatusCodes.OK)(action: => Either[Failure, _]) {    
    var headers = ctx.request.headers.filter({
      h => h.name == "Authorization"
    })
    var authorization = if (headers.length > 0) headers(0).value else ""
    println(s"Authorization: $authorization")
    if (permissionService.userHasPermission(authorization, permission)) {
      action match {
        case Right(result: Object) =>
          ctx.complete(successCode, write(result))
        case Left(error: Failure) =>
          ctx.complete(error.getStatusCode, net.liftweb.json.Serialization.write(Map("error" -> error.message)))
        case _ =>
          ctx.complete(StatusCodes.InternalServerError)
      }
    }
    else {
      var error = unauthorizedAction(authorization)
      ctx.complete(error.getStatusCode, net.liftweb.json.Serialization.write(Map("error" -> error.message)))
    }
  }
}
