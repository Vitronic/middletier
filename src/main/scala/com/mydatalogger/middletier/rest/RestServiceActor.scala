package com.mydatalogger.middletier.rest

import akka.actor.Actor
import akka.event.slf4j.SLF4JLogging
import com.mydatalogger.middletier.dao.CustomerDAO
import com.mydatalogger.middletier.domain._
import com.mydatalogger.middletier.dao.EventDAO
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
//import java.sql.Timestamp
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

  val customerService = new CustomerDAO
  
  val eventService = new EventDAO
  
  val AccessControlAllowAll = HttpHeaders.RawHeader(
    "Access-Control-Allow-Origin", "*"
  )
  
  val AccessControlAllowHeadersAll = HttpHeaders.RawHeader(
    "Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept"
  ) 
  
  implicit val executionContext = actorRefFactory.dispatcher

  implicit val liftJsonFormats = new Formats {
    val dateFormat = new DateFormat {
      val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

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
      val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
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

  //l rest = respondWithMediaType(MediaTypes.`application/json`) {
  //val rest= {
    val rest = respondWithHeaders(AccessControlAllowAll, AccessControlAllowHeadersAll) 
    {
    path("event") {
      options {
         entity(Unmarshaller(MediaTypes.`application/json`) {
            case httpEntity: HttpEntity =>
              read[Event](httpEntity.asString(HttpCharsets.`UTF-8`))
          }) {
            event: Event =>
              ctx: RequestContext =>
                handleRequest(ctx, StatusCodes.Created) {
                  log.debug("Creating event: %s".format(event))
                  eventService.create(event)
                }
          }
        } ~
      post {
          entity(Unmarshaller(MediaTypes.`application/json`) {
            case httpEntity: HttpEntity =>
              read[Event](httpEntity.asString(HttpCharsets.`UTF-8`))
          }) {
            event: Event =>
              ctx: RequestContext =>
                handleRequest(ctx, StatusCodes.Created) {
                  log.debug("Creating event: %s".format(event))
                  eventService.create(event)
                }
          }
        } ~
        get {
          
          parameters('type_ev.as[String] ?,'userName.as[String] ?,'points_ev.as[String] ?,'date_ev.as[Date] ?,'datetime_ev.as[Date] ?).as(EventSearchParameters) {
         
            searchParameters: EventSearchParameters =>
              {
                ctx: RequestContext =>
                  handleRequest(ctx) {
                    log.debug("Searching for events with parameters: %s".format(searchParameters))
                    eventService.search(searchParameters)
                  }
              }
            
          }
        }
    } ~
      path("event" / LongNumber) {
        eventId =>
          put {
            entity(Unmarshaller(MediaTypes.`application/json`) {
              case httpEntity: HttpEntity =>
                read[Event](httpEntity.asString(HttpCharsets.`UTF-8`))
            }) {
              event: Event =>
                ctx: RequestContext =>
                  handleRequest(ctx) {
                    log.debug("Updating event with id %d: %s".format(eventId, event))
                    eventService.update(eventId, event)
                  }
            }
          } ~
            delete {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Deleting event with id %d".format(eventId))
                  customerService.delete(eventId)
                }
            } ~
            get {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Retrieving evnet with id %d".format(eventId))
                  eventService.get(eventId)
                }
            }
      } ~
    path("admin") {
        
            getFromResource("admin/index.html")
        
      } ~
      pathPrefix("admin") {
          
          getFromResourceDirectory("admin")
        
      }~
      path("customer") {
      post {
        entity(Unmarshaller(MediaTypes.`application/json`) {
          case httpEntity: HttpEntity =>
            read[Customer](httpEntity.asString(HttpCharsets.`UTF-8`))
        }) {
          customer: Customer =>
            ctx: RequestContext =>
              handleRequest(ctx, StatusCodes.Created) {
                log.debug("Creating customer: %s".format(customer))
                customerService.create(customer)
              }
        }
      } ~
        get {
          parameters('firstName.as[String] ?, 'lastName.as[String] ?, 'birthday.as[Date] ?).as(CustomerSearchParameters) {
            searchParameters: CustomerSearchParameters => {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Searching for customers with parameters: %s".format(searchParameters))
                  customerService.search(searchParameters)
                }
            }
          }
        }
    } ~
      path("customer" / LongNumber) {
        customerId =>
          put {
            entity(Unmarshaller(MediaTypes.`application/json`) {
              case httpEntity: HttpEntity =>
                read[Customer](httpEntity.asString(HttpCharsets.`UTF-8`))
            }) {
              customer: Customer =>
                ctx: RequestContext =>
                  handleRequest(ctx) {
                    log.debug("Updating customer with id %d: %s".format(customerId, customer))
                    customerService.update(customerId, customer)
                  }
            }
          } ~
            delete {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Deleting customer with id %d".format(customerId))
                  customerService.delete(customerId)
                }
            } ~
            get {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Retrieving customer with id %d".format(customerId))
                  customerService.get(customerId)
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