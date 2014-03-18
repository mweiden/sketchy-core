package com.soundcloud.sketchy.util

import java.io.{ PrintStream, ByteArrayOutputStream }
import java.text.SimpleDateFormat
import org.scalatest.FlatSpec

import com.soundcloud.sketchy.events.{ UserEventKey, SketchySignal }
import com.soundcloud.sketchy.SpecHelper


/**
 * Test for logging
 */
class LoggingTest extends FlatSpec with SpecHelper {
  behavior of "The logger"

  it should "log info" in {
    new LoggingConfig {
      log.log('INFO, "add user=1: score=1.000000", stream, Nil, mockNow)
      assert(output() === fixtures("log", "info"))
    }
  }

  it should "log info with metadata" in {
    new LoggingConfig {
      val meta = List("x-category" -> "foobar")
      log.log('INFO, "foo", stream, meta, mockNow)
      assert(output() === fixtures("log", "metadata"))
    }
  }

  it should "log signals" in {
    new LoggingConfig {
      val junk = new SketchySignal(1, "Message", List(1, 2), "Junk", 1.0, mockNow)
      val meta = List("x-category" -> "sketchy_signal")

      log.log('INFO, junk.json, stream, meta, mockNow)
      assert(output() === fixtures("log", "signal"))
    }
  }

  /**
   * Internal Helpers
   */
  trait LoggingConfig extends Logging {
    override val log = new Logger(emailExceptions = false, prependTimeInfo = true)
    val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ")
    val mockNow = fmt.parse("2012-05-23 16:23:31+0000")
    val out = new ByteArrayOutputStream()
    val stream: PrintStream = new PrintStream(out);
    def output() = out.toString("UTF-8")
  }
}

