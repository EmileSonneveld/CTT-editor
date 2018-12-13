package main.scala

import scala.collection.mutable.ListBuffer

class CttOperator(val name: String, val priority: Int, val explenation: String)

object CttNode {
  val operators = List(
    new CttOperator("[]", 1, "Choice"),

    new CttOperator("|=|", 2, "Order independant"),
    new CttOperator("|||", 2, "Interleaving"),
    new CttOperator("|[]|", 2, "Interleaving with information exchange"), // priority unsure
    new CttOperator("||", 2, "Parallelism"), // priority unsure

    new CttOperator("[>", 3, "Deactivation"),

    new CttOperator(">>", 4, "Enabling"),
    new CttOperator("[]>>", 4, "Enabling with information exchange"),
    new CttOperator("|>", 4, "Suspend-resume"), // priority unsure
  )

  def maxCttPriority = {
    operators.maxBy(x => x.priority).priority
  }
}

class CttNode {
  var name = "untitled_node"
  var children: ListBuffer[CttNode] = ListBuffer[CttNode]()
  var pos = new Vector2D(-1, -1)
  var width: Double = -1 // not calculated yet
  var parent: CttNode = _

  val icons = List("abstraction", "application", "interaction", "user")

  def displayName(): String = {
    val sp = name.split(' ')
    if (icons.contains(sp.last))
      return name.substring(0, name.length - sp.last.length - 1)
    return name
  }

  def addChild(child: CttNode, index: Int = -1) = {
    child.parent = this
    var idx = index
    if (idx == -1)
      idx = children.length
    this.children.insert(idx, child)
  }

  def findTaskRightUp(): ListBuffer[CttNode] = {
    var retList = ListBuffer[CttNode]()
    def rec(n: CttNode): CttNode = {
      if (n.parent == null) return null

      var passedSelf = false // This mechanism could probably be simpler if we depend on propper CTT-normalisation
      var passedDisabelingTask = false
      for (sibling <- n.parent.children) {
        if (passedSelf) {
          if (passedDisabelingTask) {
            retList += sibling
            //return sibling
          } else {
            val op = sibling.Operator()
            if (op != null && op.name == "[>") passedDisabelingTask = true
          }
        } else {
          if (sibling == n) passedSelf = true
        }
      }
      return rec(n.parent)
    }

    rec(this)
    return retList
  }

  def minimumWidth(): Double = {
    Math.max(32, name.length * 6) // need to render in fixed width font
  }

  def Operator(): CttOperator = {
    val n = name.toLowerCase().substring(0, Math.min(4, name.length))
    val ops = CttNode.operators.sortBy(x => -x.name.length) // Otherwise []>> could be identified as []
    for (op <- ops)
      if (n.startsWith(op.name))
        return op
    return null
  }

  def GetIconName(): String = {
    val sp = name.split(' ')
    if (icons.contains(sp.last)) return sp.last + ".gif"

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
    displayName()
  }
}
