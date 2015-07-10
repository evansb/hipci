package edu.nus.hipci.util

import akka.actor.Props
import edu.nus.hipci.common.{Diff3, Component, ComponentDescriptor, GenTest}

/**
 * Analyze output and compare it to reference output, and generate a report string.
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */
object OutputAnalyzer extends ComponentDescriptor {
  val name = "OutputAnalyzer"
  val props = Props[OutputParser]
  val subComponents = List()
}


class OutputAnalyzer extends Component {
  val descriptor = OutputAnalyzer

  def analyze[K,V](output1: GenTest, output2: GenTest, reference: GenTest) = {
    Diff3(output1.path, output2.path, reference.path,
      arguments3(output1, output2, reference),
      combineSpec(output1.specs, output2.specs, reference.specs))
  }

  private def arguments3[K,V](output1: GenTest, output2: GenTest, reference: GenTest) = {
    val set1 = output1.arguments.toSet
    val set2 = output2.arguments.toSet
    val set3 = reference.arguments.toSet
    val allKeys = set1.union(set2.union(set3))
    allKeys.foldRight(List.empty[(String, Boolean, Boolean, Boolean)])({ (key, acc) =>
      (key, set1.contains(key), set2.contains(key), set3.contains(key))::acc
    })
  }

  private def combineSpec[K, V](spec1: Map[K,V], spec2: Map[K,V], spec3: Map[K,V]) = {
    val allKeys = spec1.keySet.union(spec2.keySet).union(spec3.keySet)
    allKeys.foldRight(List.empty[(K, Option[V], Option[V], Option[V])])({ (key, acc) =>
      (key, spec1.get(key), spec2.get(key), spec3.get(key))::acc
    })
  }

  override def receive = {
    case request.AnalyzeOutput(output1, output2, reference) =>
      sender ! analyze(output1, output2, reference)
    case other => super.receive(other)
  }
}

