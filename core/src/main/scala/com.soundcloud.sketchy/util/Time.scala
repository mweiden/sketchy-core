package com.soundcloud.sketchy.util

import java.util.{ Locale, TimeZone }

object Time {

  // need predictable number formats for logging and serialization
  def localize() {
    Locale.setDefault(Locale.ENGLISH)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

}
