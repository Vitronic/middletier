package com.mydatalogger.middletier.domain

import scala.slick.driver.MySQLDriver.simple._

/**
 * Event entity.
 *
 * @param id        unique id
 * @param date_ev date event
 * @param type_ev event type
 * @param points_ev  points_ev
 * @param userName  User Name
 */
case class Event(id: Option[Long], type_ev: String, userName:String, points_ev: String, date_ev: Option[java.util.Date])

/**
 * Mapped events table object.
 */
object Events extends Table[Event]("events") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def type_ev = column[String]("type_ev")
  
  def userName = column[String]("userName")

  def points_ev = column[String]("points_ev")

  def date_ev = column[java.util.Date]("date_ev", O.Nullable)

  def * = id.? ~ type_ev ~ userName ~ points_ev ~ date_ev.? <>(Event, Event.unapply _)
 
  implicit val dateTypeMapper = MappedTypeMapper.base[java.util.Date, java.sql.Date](
  {
    ud => new java.sql.Date(ud.getTime)
  }, {
    sd => new java.util.Date(sd.getTime)
  })

  val findById = for {
    id <- Parameters[Long]
    c <- this if c.id is id
  } yield c
}