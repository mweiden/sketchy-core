package com.soundcloud.sketchy.util

import java.text.SimpleDateFormat
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
import play.api.libs.json._

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import java.util.Date

package object formats {
  def camelize(scored: String): String =
    scored.split("_").foldRight("")((b, a) => b + a.capitalize)

  def snakify(camalized: String): String =
    camalized.replaceAll(
      String.format("%s|%s|%s",
        "(?<=[A-Z])(?=[A-Z][a-z])",
        "(?<=[^A-Z])(?=[A-Z])",
        "(?<=[A-Za-z])(?=[^A-Za-z])"),
      "_").toLowerCase()

  val javaDateFormat = "yyyy/MM/dd HH:mm:ss ZZZZZ"
  val jodaDateFormatter = ISODateTimeFormat.dateTimeNoMillis()


  def parseJodaDate(input: String): Option[DateTime] =
    scala.util.control.Exception.allCatch[DateTime] opt (jodaDateFormatter.parseDateTime(input))

  def parseJavaDate(input: String): Option[Date] = {
    val dateFormatter = new SimpleDateFormat(javaDateFormat)
    scala.util.control.Exception.allCatch[Date] opt (dateFormatter.parse(input))
  }
}

package object readers {
  import formats._

  implicit val jodaISODateReads: Reads[DateTime] = new Reads[DateTime] {

    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsNumber(d) => JsSuccess(new DateTime(d.toLong))
      case JsString(s) => parseJodaDate(s) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jodadate.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date"))))
    }
  }

  implicit val javaDateReads: Reads[Date] = new Reads[Date] {

    def reads(json: JsValue): JsResult[Date] =
      json match {
        case JsNumber(d) => JsSuccess(new Date(d.toLong))
        case JsString(s) => parseJavaDate(s) match {
          case Some(d) => JsSuccess(d)
          case None => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date.format"))))
        }
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date"))))
      }

  }
}

package object writers {
  import formats._

  implicit val jodaISODateWrites: Writes[DateTime] = new Writes[DateTime] {
    def writes(d: DateTime): JsValue = {
      JsString(jodaDateFormatter.print(d))
    }
  }

  implicit val javaDateWrites: Writes[Date] = new Writes[Date] {
    def writes(d: Date): JsValue = {
      val dateFormatter = new SimpleDateFormat(javaDateFormat)
      JsString(dateFormatter.format(d))
    }
  }
}
