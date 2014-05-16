package com.soundcloud.sketchy.events

import java.text.SimpleDateFormat

import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization._
import net.liftweb.util.StringHelpers

import com.soundcloud.sketchy.util.Formatting

/**
 * Parsing and serializing of events
 */
trait Transform {
  implicit val formats: Formats = new DefaultFormats {
    val fmt = "yyyy/MM/dd HH:mm:ss ZZZZZ"
    override def dateFormatter = new SimpleDateFormat(fmt)

    override val typeHints = new ShortTypeHints(classOf[Action] :: Nil)
    override val typeHintFieldName = "type"
  }

  def transform(source: JValue, fn: (String) => String): JValue = {
    source.transform { case JField(x, y) => JField(fn(x), y) }
  }
}

trait Parsing extends Transform {
  def extractor(body: String) =
    transform(JsonParser.parse(body), Formatting.camelized _)
}

trait Serializing extends Transform {
  def json: String = compact(representation)
  def jsonPretty: String = pretty(representation)

  def representation =
    render(transform(decompose(this)(formats), Formatting.scored _))

}

trait Serializer extends Transform {
  def serialize(t: Any) = compact(render(decompose(t) transform {
    case JField(n, v) => JField(StringHelpers.snakify(n), v) }))
}
