package com.soundcloud.sketchy.util


abstract trait Classifier {

  def predict(str: String): Double

}

abstract trait Tokenizer {
  def fingerprint(str: String): List[Int]

  def dist(set1: List[Int], set2: List[Int]): Double

  def featurize(str: String): List[Long]
}

