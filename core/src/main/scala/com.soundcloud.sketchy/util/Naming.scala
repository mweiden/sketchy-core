package com.soundcloud.sketchy.util

trait Naming {
  def nameArray = getClass.getName.split('.')
  def typeName = nameArray(nameArray.length - 1)
  def subtypeName = nameArray(nameArray.length - 2)
  def networkName = nameArray(nameArray.length - 3)
}
