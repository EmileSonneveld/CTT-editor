package main.scala

import scala.collection.mutable.ListBuffer

class CttNode {
  val operators = List("|=|", "[]", "|||", "|[]|", "||", "[>", ">>", "[]>>", "|>")


  var name = "untitled_node"
  var children: ListBuffer[CttNode] = ListBuffer[CttNode]()
  var pos = new Vector2D(-1, -1)
  var width: Double = -1 // not calculated yet

  def minimumWidth(): Double = {
    Math.max(32, name.length * 6) // need to render in fixed width font
  }

  def IsOpperator(): Boolean = {
    val n = name.toLowerCase()
    operators.contains(n)
  }

  def GetIconName(): String = {
    if (children.size > 0) return "abstraction.gif"
    val n = name.toLowerCase()
    if (IsOpperator()) return ""
    if (n.startsWith("show")
      || n.startsWith("check")
      || n.startsWith("print")
      || n.startsWith("is")) return "application.gif"
    return "interaction.gif"
  }

  override def toString = {
    name
  }
}
