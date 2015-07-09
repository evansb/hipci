package edu.nus.hipci.common

/**
 * 3 way diff between tests
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
case class Diff3[K,V](output1: String, output2: String, reference: String,
                      arguments: List[(String, Boolean, Boolean, Boolean)],
                      specs: List[(K, Option[V], Option[V], Option[V])])
