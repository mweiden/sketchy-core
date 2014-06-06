package com.soundcloud.example.util

import com.soundcloud.sketchy.util._

import nak.NakContext._
import nak.core.FeaturizedClassifier


class SVMClassifier(modelPath: String) extends Classifier {

  val classifier =
    loadClassifier[FeaturizedClassifier[String,String]](modelPath)

  def predict(str: String): (Int,Double) = {
    val prob = classifier.evalRaw(str)(1)
    val label = if (prob > 0.5) 1 else 0
    (label, prob)
  }

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
    val set1 = fingerprint(str1).toSet
    val set2 = fingerprint(str2).toSet
    val unionsize = set1.union(set2).size
    (unionsize - (set1 & set2).size) / unionsize.toDouble
  }

}

