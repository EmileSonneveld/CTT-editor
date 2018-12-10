package main.scala

import scala.collection.mutable.ListBuffer

class CttOperator(val name:String, val priority:Int, val explenation:String)


class CttNode {
  val operators = List(
    new CttOperator("[]"  , 1, "Choice"),

    new CttOperator("|=|" , 2, "Order independant"),
    new CttOperator("|||" , 2, "Interleaving"),
    new CttOperator("|[]|", 2, "Interleaving with information exchange"), // priority unsure
    new CttOperator("||"  , 2, "Parallelism"), // priority unsure

    new CttOperator("[>"  , 3, "Deactivation"),

    new CttOperator(">>"  , 4, "Enabling"),
    new CttOperator("[]>>", 4, "Enabling with information exchange"),
    new CttOperator("|>"  , 4, "Suspend-resume"), // priority unsure
  )

  var name = "untitled_node"
  var children: ListBuffer[CttNode] = ListBuffer[CttNode]()
  var pos = new Vector2D(-1, -1)
  var width: Double = -1 // not calculated yet

  def minimumWidth(): Double = {
    Math.max(32, name.length * 6) // need to render in fixed width font
  }

  def Operator(): CttOperator = {
    val n = name.toLowerCase().substring(0, Math.min(4, name.length))
    val ops = operators.sortBy(x => -x.name.length) // Otherwise []>> could be identified as []
    for(op<-ops)
      if(n.startsWith(op.name))
        return op
    return null
  }

  def GetIconName(): String = {
    if (children.size > 0) return "abstraction.gif"
    val n = name.toLowerCase()
    if (Operator() != null) return ""
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
