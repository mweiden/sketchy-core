package com.soundcloud.sketchy.util


/**
 * text utilities
 *
 * Contained in this file are interfaces for turning text into hashed integer
 * representations and classifying text as spam.
 *
 * For a production use of Sketchy you will likely want to implement your own,
 * but the implementation in the example project provides a good starting
 * point
 */
abstract trait Classifier {

  // should return a class label for a string and the probability of that label
  def predict(str: String): (Int, Double)

}

abstract trait Tokenizer {

  // should return a representation of the string as a list of ints
  def fingerprint(str: String): List[Int]

  // should return a distance between two integer sets
  def dist(set1: List[Int], set2: List[Int]): Double

}

