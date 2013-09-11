package com.soundcloud.sketchy.util

object Formatting {
  def camelized(scored: String): String =
    scored.split("_").foldRight("")((b, a) => b + a.capitalize)

  def scored(camalized: String): String =
    camalized.replaceAll(
      String.format("%s|%s|%s",
        "(?<=[A-Z])(?=[A-Z][a-z])",
        "(?<=[^A-Z])(?=[A-Z])",
        "(?<=[A-Za-z])(?=[^A-Za-z])"),
      "_").toLowerCase()
}
