package com.soundcloud.example.util

import org.scalatest.FlatSpec

import com.soundcloud.example.SpecHelper

/**
 * Simplest possible test of imported Classifier.
 */
class TextTest extends FlatSpec with SpecHelper {
  behavior of "The classifier library"

  val classifier = svmClassifier("junk")

  it should "predict the probability of text being spam" in {
    assert(classifier.predict("VIAGRA IS GREAT OMG") === (1,0.7447720620652247))
  }

  behavior of "The tokenizer and Fingerprinter"

  val tokenizer = new SimpleTokenizer

  it should "fingerprint text" in {
    assert(tokenizer.fingerprint("Hello! Hi.") === List(469940726, 1747180236))
  }


  it should "calculate the jaccard coefficient between two sets" in {
    assert(tokenizer.dist(List(2,3), List(2,3)) === 0.0)
    assert(tokenizer.dist(List(2),   List(2,3)) === 0.5)
    assert(tokenizer.dist(List(0,1), List(2,3)) === 1.0)
  }
}
