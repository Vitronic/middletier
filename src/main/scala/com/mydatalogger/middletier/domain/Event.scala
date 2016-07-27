package com.mydatalogger.middletier.domain

import scala.slick.driver.MySQLDriver.simple._
import org.joda.time.DateTime;
import slick.lifted.MappedTypeMapper
import java.sql.Date
import org.joda.time.DateTime
import slick.lifted.TypeMapper.DateTypeMapper


/**
 * Event entity.
 *
 * @param id        unique id
 * @param date_ev date event
 * @param datetime_ev date event
 * @param type_ev event type
 * @param points_ev  points_ev
 * @param userName  User Name
 */
case class Event(id: Option[Long], type_ev: String, userName:String, points_ev: String, date_ev: Option[java.util.Date],datetime_ev: Option[java.util.Date])

/**
 * Mapped events table object.
 */
object Events extends Table[Event]("events") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def type_ev = column[String]("type_ev")
  
  def userName = column[String]("userName")

  def points_ev = column[String]("points_ev")

  def date_ev = column[java.util.Date]("date_ev", O.Nullable)
  
  def datetime_ev = column[java.util.Date]("datetime_ev", O.Nullable)

  def * = id.? ~ type_ev ~ userName ~ points_ev ~ date_ev.? ~ datetime_ev.? <>(Event, Event.unapply _)
   
   
  implicit def date2dateTime = MappedTypeMapper.base[DateTime, Date] (
    dateTime => new Date(dateTime.getMillis),
    date => new DateTime(date)
  )
  
  implicit val dateTypeMapper = MappedTypeMapper.base[java.util.Date, java.sql.Timestamp](
  {
    ud => new java.sql.Timestamp(ud.getTime)
  }, {
    sd => new java.util.Date(sd.getTime)
  })

  val findById = for {
    id <- Parameters[Long]
    c <- this if c.id is id
  } yield c
}