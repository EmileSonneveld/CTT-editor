package main.scala

import org.scalajs.dom
import dom.document
import org.scalajs.dom.raw.{SVGImageElement, SVGLineElement, SVGTextElement}
import scala.collection.mutable.ListBuffer
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.control.Breaks._
import scala.collection.mutable.Stack


class Vector2D(_x: Double, _y: Double) {
  var x: Double = _x
  var y: Double = _y
}


object CttEditor {

  val ctt_code =
    """
acces_schedule
	view_schedule
		select_doctor_task
			browse_department
				show_department
				[]>>
				select_sub_department
			[>
			submit_department
			[]>>
			select_doctor_in_department
				filter_doctors
					enter_word
					|[]|
					show_doctors_containing_word
				[>
				select_doctor
		[]>>
		use_this_doctor
		[]>>
		show_schedule
	[>
	quit
"""

  def main(args: Array[String]): Unit = {
    val ctt = linear_parse_ctt(ctt_code.replace("\r", ""))
    val str = print_ctt(ctt)
    println(str)
    calculateWidth(ctt)
    calculatePosition(ctt)
    val el = render_ctt_to_svg(ctt)
    dom.document.body.appendChild(el)
  }

  val operators = List("|=|", "[]", "|||", "|[]|", "||", "[>", ">>", "[]>>", "|>")

  class CttNode {
    var name = "untitled_node"
    var children: ListBuffer[CttNode] = ListBuffer[CttNode]()
    var pos = new Vector2D(-1, -1)
    var width: Double = -1 // not calculated yet

    def minimumWidth(): Double = {
      Math.max(32, name.length * 6) // need to render in fixed width font
    }

    def GetIconName(): String = {
      if (children.size > 0) return "abstraction.gif"
      if(operators.contains(name)) return ""
      return "interaction.gif"
    }
  }

  def calculateWidth(node: CttNode): Unit = {
    val gap = 5
    val padding: Double = 5.0

    var acc_children: Double = 0.0
    for (child <- node.children) {
      calculateWidth(child)
      acc_children += child.width
    }
    if (node.children.length > 1)
      acc_children += gap * (node.children.length - 1)

    node.width = Math.max(node.minimumWidth(), acc_children) + (padding * 2)
  }

  def vectorAdd(a: Vector2D, b: Vector2D): Vector2D = {
    new Vector2D(a.x + b.x, a.y + b.y)
  }

  def calculatePosition(node: CttNode, offset: Vector2D = new Vector2D(0, 0)): Unit = {
    val sizePerLayer = 60

    node.pos.x = offset.x + node.width / 2
    node.pos.y = offset.y

    for (child <- node.children) {
      calculatePosition(child, vectorAdd(offset, new Vector2D(0, sizePerLayer)))
      offset.x += child.width
    }
  }

  def render_ctt_to_svg(node: CttNode): org.scalajs.dom.raw.Element = {
    val el = dom.document.createElementNS("http://www.w3.org/2000/svg", "svg")
    el.setAttribute("width", "2000")
    el.setAttribute("height", "2000")

    def render_recurse(n: CttNode): Unit = {

      var prevChild: CttNode = null
      for (child <- n.children) {
        if (prevChild != null) {
          val line = document.createElementNS("http://www.w3.org/2000/svg", "line").asInstanceOf[SVGLineElement]
          line.setAttribute("x1", "" + prevChild.pos.x)
          line.setAttribute("y1", "" + prevChild.pos.y)
          line.setAttribute("x2", "" + child.pos.x)
          line.setAttribute("y2", "" + child.pos.y)
          line.style.strokeWidth = "2"
          line.style.stroke = "rgb(0,0,0)"
          el.appendChild(line)
        }
        {
          val line = document.createElementNS("http://www.w3.org/2000/svg", "line").asInstanceOf[SVGLineElement]
          line.setAttribute("x1", "" + n.pos.x)
          line.setAttribute("y1", "" + n.pos.y)
          line.setAttribute("x2", "" + child.pos.x)
          line.setAttribute("y2", "" + child.pos.y)
          line.style.strokeWidth = "2"
          line.style.stroke = "rgb(0,0,0)"
          el.appendChild(line)
        }
        render_recurse(child)
        prevChild = child
      }


      val icon = n.GetIconName()
      if(icon == ""){

        val rect = document.createElementNS("http://www.w3.org/2000/svg", "rect")
        rect.setAttribute("x", "" + (n.pos.x - 16))
        rect.setAttribute("y", "" + (n.pos.y - 16))
        rect.setAttribute("width", "32")
        rect.setAttribute("height", "32")
        rect.setAttribute("fill", "#FFFFFF") // to hide lines passing behind it
        el.appendChild(rect)
      }else{

        val img = document.createElementNS("http://www.w3.org/2000/svg", "image").asInstanceOf[SVGImageElement]
        img.setAttribute("x", "" + (n.pos.x - 16))
        img.setAttribute("y", "" + (n.pos.y - 16))
        img.setAttribute("width", "32")
        img.setAttribute("height", "32")
        img.setAttributeNS("http://www.w3.org/1999/xlink","href", icon)
        img.setAttributeNS(null, "visibility", "visible")
        el.appendChild(img)
      }

      val text = document.createElementNS("http://www.w3.org/2000/svg", "text").asInstanceOf[SVGTextElement]
      text.setAttribute("x", "" + (n.pos.x - n.name.length * 3))
      text.setAttribute("y", "" + (n.pos.y + 16))
      text.setAttribute("width", "999")
      text.setAttribute("height", "32")
      text.innerHTML = n.name
      text.style.fontFamily = "monospace"
      el.appendChild(text)
    }

    render_recurse(node)
    return el
  }

  def isEmpty(x: String): Boolean = x == null || x.isEmpty

  def linear_parse_ctt(code: String): CttNode = {
    val root = new CttNode
    root.name = "standard_root_node"
    val stack = new Stack[CttNode]

    var currentNode = root

    var indentLevel: Int = -1

    var currentCharIndex: Int = 0
    var nextCharIndex = 0 // ignore first itteration.

    while (nextCharIndex != -1) {
      val line = code.substring(currentCharIndex, nextCharIndex)
      if (!isEmpty(line)) {
        println(line)
        val leading_tabs = count_leading_tabs(line)
        var node = new CttNode
        node.name = line.substring(leading_tabs)
        if (leading_tabs == indentLevel + 1) {
          stack.push(currentNode)
          indentLevel = leading_tabs
        } else if (leading_tabs == indentLevel) {
          // nothing special to do
        } else if (leading_tabs < indentLevel) {
          shrink_stack(stack, leading_tabs + 1)
        } else {
          throw new Exception("Something went wrong")
        }

        currentNode = node
        stack.top.children += node

      }
      currentCharIndex = nextCharIndex + 1
      nextCharIndex = code.indexOf("\n", currentCharIndex + 1) // seek after newline
    }

    return root
  }

  def shrink_stack(stack: Stack[CttNode], size: Int): Unit = {
    assert(stack.size >= size)
    for (_ <- 0 until stack.size - size) {
      stack.pop()
    }
  }

  def print_ctt(node: CttNode): String = {
    val sb = new StringBuilder

    def print_recurse(n: CttNode, indentLevel: Int = 0): Unit = {
      sb.append(("\t" * indentLevel) + n.name + "\n")
      for (child <- n.children) {
        print_recurse(child, indentLevel + 1)
      }
    }

    print_recurse(node)
    val str = sb.toString()
    return str
  }

  def count_leading_tabs(str: String): Int = {
    var count = 0
    for (i <- 0 until str.length) {
      if (str(i) == '\t')
        count += 1
      else
        return count
    }
    return count
  }


  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode = document.createElement("p")
    val textNode = document.createTextNode(text)
    parNode.appendChild(textNode)
    targetNode.appendChild(parNode)
  }

  @JSExportTopLevel("addClickedMessage")
  def addClickedMessage(): Unit = {
    appendPar(document.body, "You clicked the button!")
  }

}
