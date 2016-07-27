package com.mydatalogger.middletier.domain

import java.util.Date

/**
 * Events search parameters.
 *
 * @param userName userName
 * @param type_ev  type event
 * @param date_ev  date event
 * @param points  points
 */


            
case class EventSearchParameters(type_ev: Option[String] = None,
                                userName: Option[String] = None,                                
                                points_ev: Option[String] = None,
                                    date_ev: Option[Date] = None)