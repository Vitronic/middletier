package com.mydatalogger.middletier.dao

import com.mydatalogger.middletier.config.Configuration
import com.mydatalogger.middletier.domain._
import java.sql._
import scala.Some
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
import scala.slick.driver.MySQLDriver.simple._
import slick.jdbc.meta.MTable


/**
 * Provides DAL for Customer entities for MySQL database.
 */
class EventDAO extends Configuration {

   
  
  // init Database instance
  private val db = Database.forURL(url = "jdbc:mysql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  // create tables if not exist
  db.withSession {
    if (MTable.getTables("events").list().isEmpty) {
      Events.ddl.create
    }
  }

  /**
   * Saves Events entity into database.
   *
   * @param event event entity to
   * @return saved event entity
   */
  def create(event: Event): Either[Failure, Event] = {
    try {
      val id = db.withSession {
        Events returning Events.id insert event
      }
      Right(event.copy(id = Some(id)))
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Updates event entity with specified one.
   *
   * @param id       id of the customer to update.
   * @param event updated event entity
   * @return updated event entity
   */
  def update(id: Long, event: Event): Either[Failure, Event] = {
    try
      db.withSession {
        Events.where(_.id === id) update event.copy(id = Some(id)) match {
          case 0 => Left(notFoundError(id))
          case _ => Right(event.copy(id = Some(id)))
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
  def delete(id: Long): Either[Failure, Event] = {
    try {
      db.withTransaction {
        val query = Events.where(_.id === id)
        val events = query.run.asInstanceOf[List[Event]]
        events.size match {
          case 0 =>
            Left(notFoundError(id))
          case _ => {
            query.delete
            Right(events.head)
          }
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Retrieves specific event from database.
   *
   * @param id id of the event to retrieve
   * @return event entity with specified id
   */
  def get(id: Long): Either[Failure, Event] = {
    try {
      db.withSession {
        Events.findById(id).firstOption match {
          case Some(event: Event) =>
            Right(event)
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
   * Retrieves list of customers with specified parameters from database.
   *
   * @param params search parameters
   * @return list of events that match given parameters
   */
  def search(params: EventSearchParameters): Either[Failure, List[Event]] = {
    implicit val typeMapper = Events.dateTypeMapper

    try {
      db.withSession {
        val query = for {
          event <- Events if {
          Seq(
            params.date_ev.map(event.date_ev is _),
            params.type_ev.map(event.type_ev is _),
            params.points_ev.map(event.points_ev is _),
            params.userName.map(event.userName is _),
            params.datetime_ev.map(event.datetime_ev is _)
          ).flatten match {
            case Nil => ConstColumn.TRUE
            case seq => seq.reduce(_ && _)
          }
        }
        } yield event

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
   * Produce event not found error description.
   *
   * @param eventId id of the customer
   * @return not found error description
   */
  protected def notFoundError(eventId: Long) =
    Failure("Event with id=%d does not exist".format(eventId), FailureType.NotFound)
}