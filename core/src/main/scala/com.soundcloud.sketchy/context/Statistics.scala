package com.soundcloud.sketchy.context

import java.util.Date

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.events.{
  Event,
  UserEventKey,
  UserEvent
}


/**
 * Sufficient statistic. A user window is a Seq of these datums.
 */
abstract class Statistics {
  def key: UserEventKey
  def marshalled: String
}

/**
 * Sufficient statistics for Junk messages. This is the confidence calculated
 * by the classifier
 */
case class JunkStatistics(
  key: UserEventKey,
  label: Int,
  probability: Double) extends Statistics {

  def marshalled: String =
    Statistics.marshal("junk", key.marshalled, label.toString, probability.toString)
}

/**
 * Bulk detector sufficient statistics. The fingerprinter is used to represent
 * a text as a set of hashes. Comparing hashes is then a case of calculating
 * the Jaccard coefficient over both; A-B / A+B
 */
case class BulkStatistics(
  key: UserEventKey,
  fingerprints: List[Int]) extends Statistics {

  def marshalled: String =
    Statistics.marshal("bulk", key.marshalled, fingerprints.mkString(":"))
}

/**
 * Report statistics to store user reports.
 */
case class SpamReportStatistics(
  key: UserEventKey,
  reporterId: Int,
  originType: String,
  originCreatedAt: Date,
  lastSignaledAt: Date) extends Statistics {

  def marshalled: String =
    Statistics.marshal("report", key.marshalled, reporterId.toString, originType,
      originCreatedAt.getTime.toString, lastSignaledAt.getTime.toString)
}

/**
 * Label statistics to store spam/non-spam label information.
 */
case class LabelStatistics(
  key: UserEventKey,
  userId: Int,
  spam: Boolean) extends Statistics {

  def marshalled: String =
    Statistics.marshal("label", key.marshalled, userId.toString, spam.toString)
}

/**
 * Batch statistics to store recently active users.
 */
case class BatchStatistics(
  key: UserEventKey) extends Statistics {

  def marshalled: String =
    Statistics.marshal("batch", key.marshalled)
}

/**
 * Ratio statistics store a total number of actions of a given kind
 */
case class RatioStatistics(
  key: UserEventKey,
  numerator: Double,
  denominator: Double) extends Statistics {

  def marshalled: String =
    Statistics.marshal("ratio", key.marshalled, numerator.toString, denominator.toString)
}


/**
 * Parsing
 */
abstract trait StatisticsParsing {
  def unmarshal(s: String): Statistics
}

object Statistics extends StatisticsParsing {
  def unmarshal(statistics: String): Statistics = {
    statistics match {
      case s if s.startsWith("junk") =>
        JunkStatistics.unmarshal(s)
      case s if s.startsWith("bulk") =>
        BulkStatistics.unmarshal(s)
      case s if s.startsWith("report") =>
        SpamReportStatistics.unmarshal(s)
      case s if s.startsWith("label") =>
        LabelStatistics.unmarshal(s)
      case s if s.startsWith("batch") =>
        BatchStatistics.unmarshal(s)
      case s if s.startsWith("ratio") =>
        RatioStatistics.unmarshal(s)
    }
  }

  def encode(str: String) =
    new String(org.apache.commons.codec.binary.Base64.encodeBase64(str.getBytes()))
  def decode(str: String) =
    new String(org.apache.commons.codec.binary.Base64.decodeBase64(str))

  def marshal(label: String, strList: String*): String =
    (label +: strList.map(str => encode(str)).toList).mkString("|")

  def unpack(str: String): List[String] = {
    val eles = str.split('|')
    if (eles.nonEmpty) {
      val fields = eles.tail
      eles.head +: fields.map(field => decode(field)).toList
    } else {
      List()
    }
  }
}

object JunkStatistics extends StatisticsParsing {
  def unmarshal(statistics: String): JunkStatistics = {
    val List(sKind, sKey, sLabel, sConfidence) = Statistics.unpack(statistics)
    val key: UserEventKey = UserEventKey.unmarshal(sKey)
    val label: Int = sLabel.toInt
    val confidence: Double = sConfidence.toDouble

    JunkStatistics(key, label, confidence)
  }
}

object BulkStatistics extends StatisticsParsing {
  def unmarshal(statistics: String): BulkStatistics = {
    val List(sKind, sKey, sFingerprints) = Statistics.unpack(statistics)
    val key: UserEventKey = UserEventKey.unmarshal(sKey)
    val fingerprints: List[Int] = sFingerprints.split(':').map(_.toInt).toList

    BulkStatistics(key, fingerprints)
  }
}

object SpamReportStatistics extends StatisticsParsing {
  def unmarshal(statistics: String): SpamReportStatistics = {
    val List(sKind, sKey, sReporterId, originType, sCreatedAt, sSignaledAt) =
      Statistics.unpack(statistics)
    val key: UserEventKey = UserEventKey.unmarshal(sKey)
    val reporterId: Int = sReporterId.toInt
    val createdAt: Date = new Date(sCreatedAt.toLong)
    val signaledAt: Date = new Date(sSignaledAt.toLong)

    SpamReportStatistics(key, reporterId, originType, createdAt, signaledAt)
  }
}

object LabelStatistics extends StatisticsParsing {
  def unmarshal(statistics: String): LabelStatistics = {
    val List(sKind, sKey, sUserId, sSpam) = Statistics.unpack(statistics)
    val key: UserEventKey = UserEventKey.unmarshal(sKey)
    val userId: Int = sUserId.toInt
    val spam: Boolean = sSpam.toBoolean

    LabelStatistics(key, userId, spam)
  }
}

object BatchStatistics extends StatisticsParsing {
  def unmarshal(statistics: String): BatchStatistics = {
    val List(sKind, sKey) = Statistics.unpack(statistics)
    val key: UserEventKey = UserEventKey.unmarshal(sKey)

    BatchStatistics(key)
  }
}

object RatioStatistics extends StatisticsParsing {
  def unmarshal(statistics: String): RatioStatistics = {
    val List(sKind, sKey, numerator, denominator) = Statistics.unpack(statistics)
    val key: UserEventKey = UserEventKey.unmarshal(sKey)

    RatioStatistics(key, numerator.toDouble, denominator.toDouble)
  }
}

/**
 * Map Statistics Classes to their parsing objects.
 */
object StatisticsParserMappings {
  case class ClassFunctionMap[S <: Statistics](func: String => S)
  implicit val jumap = ClassFunctionMap[JunkStatistics](JunkStatistics.unmarshal)
  implicit val bumap = ClassFunctionMap[BulkStatistics](BulkStatistics.unmarshal)
  implicit val bamap = ClassFunctionMap[BatchStatistics](BatchStatistics.unmarshal)
  implicit val srmap = ClassFunctionMap[SpamReportStatistics](SpamReportStatistics.unmarshal)
  implicit val lamap = ClassFunctionMap[LabelStatistics](LabelStatistics.unmarshal)
  implicit val stmap = ClassFunctionMap[Statistics](Statistics.unmarshal)
  implicit val rtmap = ClassFunctionMap[RatioStatistics](RatioStatistics.unmarshal)
}

