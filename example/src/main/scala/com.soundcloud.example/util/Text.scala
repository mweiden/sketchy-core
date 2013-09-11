package com.soundcloud.example.util

import com.soundcloud.sketchy.util._

import nak.NakContext._
import nak.core.FeaturizedClassifier


class SVMClassifier(modelPath: String) extends Classifier {

  val classifier =
    loadClassifier[FeaturizedClassifier[String,String]](modelPath)

  def predict(str: String): Double = classifier.evalRaw(str)(1)

}

class SimpleTokenizer extends Tokenizer {

  def fingerprint(str: String): List[Int] =
    str.replaceAll("\\p{Punct}", " ")
      .toLowerCase
      .trim
      .split("\\s+")
      .map(tok => scala.util.hashing.MurmurHash3.stringHash(tok)).toList

  def dist(set1: List[Int], set2: List[Int]): Double = {
    val unionsize = set1.toSet.union(set2.toSet).size
    (unionsize - (set1.toSet & set2.toSet).size) / unionsize.toDouble
  }

  def dist(str1: String, str2: String): Double = {
    val set1 = featurize(str1).toSet
    val set2 = featurize(str2).toSet
    val unionsize = set1.union(set2).size
    (unionsize - (set1 & set2).size) / unionsize.toDouble
  }

  def featurize(str: String): List[Long] =
    fingerprint(str).map(i => unsigned(i))

  private def unsigned(x: Int) =
    0.until(32).reverse.map(shift => x & 1L << shift).sum
}

