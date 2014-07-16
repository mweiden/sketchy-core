package com.soundcloud.sketchy.events

import java.text.SimpleDateFormat
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
import play.api.libs.json._


object JSON {
  import com.soundcloud.sketchy.util.formats._

  // use: fromJson(str).as[<class>]
  def fromJson(json: String) = Json.parse(json) match {
    case j: JsObject => Some(transform(j, pconv))
    case _ => None
  }

  def json[T]
    (a: T)
    (implicit writer: Writes[T]): String =
    Json.stringify(representation(a))

  def jsonPretty[T]
    (a: T)
    (implicit writer: Writes[T]): String =
    Json.prettyPrint(representation(a))


  def representation[T](a: T)(implicit writer: Writes[T]) = Json.toJson(a) match {
    case j: JsObject => transform(j, sconv)
    case _ => throw new java.lang.RuntimeException("ERROR OK??")
  }

  def transform(
    jsObj: JsObject,
    fn: ((String, JsValue)) => (String, JsValue)): JsObject =
    JsObject(jsObj.fields.map(fn).toSeq)

  val pconv = (t: (String,JsValue)) => (camelize(t._1), t._2)
  val sconv = (t: (String,JsValue)) => (snakify(t._1),  t._2)
}
