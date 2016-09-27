package models.graph

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Graph(name: String, nodes: List[GraphNode], edges: List[GraphEdge]) {
  val inputs = nodes.filter( node => node.nodeType == "Input" )
  val outputs = nodes.filter( node => node.nodeType == "Output" )
  val intermediates = nodes.filter( node => node.nodeType == "Intermediate" )
}

case class GraphNode(nodeIndex: Int, name: String, nodeType: String, usage: String = "combined", description: String = "")

case class GraphEdge(id: String, sourceIndex: Int, targetIndex: Int, edgeType: String, value: String)

object GraphModelJsonSerializers {

  implicit val writesGraphToJson: Writes[Graph] = (
    (JsPath \ "name").write[String] and
    (JsPath \ "nodes").lazyWrite(Writes.seq[GraphNode]) and
    (JsPath \ "edges").lazyWrite(Writes.seq[GraphEdge]) and
    (JsPath \ "inputs").lazyWrite(Writes.seq[Int]) and
    (JsPath \ "outputs").lazyWrite(Writes.seq[Int]) and
    (JsPath \ "intermediates").lazyWrite(Writes.seq[Int])
  )( g => (g.name, g.nodes, g.edges, g.inputs.map( n => n.nodeIndex ), g.outputs.map( n => n.nodeIndex ), g.intermediates.map( n => n.nodeIndex )) )

  implicit val writesGraphNodeToJson: Writes[GraphNode] = (
    (JsPath \ "nodeIndex").write[Int] and
    (JsPath \ "name").write[String] and
    (JsPath \ "nodeType").write[String] and
    (JsPath \ "usage").write[String] and
    (JsPath \ "description").write[String]
  )(unlift(GraphNode.unapply))

  implicit val writesGrpahEdgeToJson: Writes[GraphEdge] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "source").write[Int] and
    (JsPath \ "target").write[Int] and
    (JsPath \ "edgeType").write[String] and
    (JsPath \ "value").write[String]
  )(unlift(GraphEdge.unapply))

}
