package services

import models.graph.{Graph, GraphEdge, GraphNode}
import org.scalarules.dsl.nl.grammar.Berekening
import org.scalarules.engine.DerivationTools._
import org.scalarules.engine.{DefaultDerivation, Fact, SubRunDerivation}

object DerivationsToGraphModel {

  def convert(derivation: Berekening): Graph = {
    val derivationsWithInfo = derivation.berekeningen.map(d => (d, List(), d.input))

    val nodes = (computeAllInputs(derivation.berekeningen) ++ computeAllOutputs(derivation.berekeningen)).map( _.name ).toList
    val edges = derivationsWithInfo.flatMap(d => {
      val (derivation, conditionInputs, evaluationInputs) = d

      derivation.input.map(i => (i.name, derivation.output.name, determineEdgeType(i, conditionInputs, evaluationInputs)) )
    } )

    val inputs = (computeAllInputs(derivation.berekeningen) -- computeAllOutputs(derivation.berekeningen)).map( _.name )
    val outputs = (computeAllOutputs(derivation.berekeningen) -- computeAllInputs(derivation.berekeningen)).map( _.name )

    val nodeToExpressionsMap = derivation.berekeningen.map( d => (d.output.name, d match {case der: DefaultDerivation => der.operation.toString; case der: SubRunDerivation => "SubRun"}) ).toMap

    def nodeToExpressions(nodeName: String) = if (nodeToExpressionsMap contains nodeName) nodeToExpressionsMap(nodeName) else ""
    def nodeToType(nodeName: String): String = if (inputs contains nodeName) "Input" else if (outputs contains nodeName) "Output" else "Intermediate"

    val nodesWithIndex = nodes.zipWithIndex
    val nodeToIndexMap = nodesWithIndex.toMap

    val nodeModels = nodesWithIndex.map{ case (n, i) => GraphNode(i, n, nodeToType(n), nodeToExpressions(n)) }
    val edgeModels = edges.map{ case (source, target, typeOfNode) => buildEdge(source, target, typeOfNode, nodeToIndexMap) }

    Graph("nom", nodeModels, edgeModels)
  }

  private def determineEdgeType(input: Fact[Any], conditionInputs: List[Fact[Any]], evaluationInputs: List[Fact[Any]]) = (conditionInputs contains input, evaluationInputs contains input) match {
    case (true, true) => "combined"
    case (true, false) => "condition"
    case (false, true) => "evaluation"
    case _ => throw new IllegalArgumentException("An input Fact was determined which was not actually part of the sets of inputs for the Derivation. Something is very wrong")
  }

  private def buildEdge(sourceNode: String, targetNode: String, typeOfEdge: String, nodeToIndexMap: Map[String, Int]): GraphEdge =
    GraphEdge(sourceNode + "-" + targetNode, nodeToIndexMap(sourceNode), nodeToIndexMap(targetNode), typeOfEdge, "1")

}
