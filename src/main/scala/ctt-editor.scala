package main.scala

import org.scalajs.dom
import dom.{Event, XMLHttpRequest, document}
import org.scalajs.dom.raw._

import scala.collection.mutable.ListBuffer
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.control.Breaks._
import scala.collection.mutable.Stack


class Vector2D(_x: Double, _y: Double) {
  var x: Double = _x
  var y: Double = _y
}


object CttEditor {

  var cttArea: HTMLTextAreaElement = _
  var cttHolder: Element = _

  def main(args: Array[String]): Unit = {
    //if(dom.document == null) return

    cttHolder = dom.document.body.querySelector("#ctt-holder")
    cttArea = dom.document.body.querySelector("#ctt-texarea").asInstanceOf[HTMLTextAreaElement]
    cttArea.addEventListener("change", cttChanged)
    cttArea.addEventListener("keyup", cttChanged)

    val oReq = new XMLHttpRequest()
    oReq.addEventListener("load", reqListener)
    oReq.open("GET", "example.txt") //, async = false)
    oReq.send()
  }

  def cttChanged(evt: Event): Unit = {
    println("cttChanged")

    var ctt_code = cttArea.value
    var ctt: CttNode = null
    try {
      ctt = linear_parse_ctt(ctt_code)
      val str = print_ctt(ctt)
      //println(str)
      calculateWidth(ctt)
      calculatePosition(ctt)
    }
    catch {
      case ex: Exception => println(ex)
    }

    //cttHolder = dom.document.body.querySelector("#ctt-holder")
    while (cttHolder.firstChild != null) {
      cttHolder.removeChild(cttHolder.firstChild);
    }

    val el = render_ctt_to_svg(ctt.children(0))
    cttHolder.appendChild(el)
  }

  def reqListener(evt: Event): Unit = {
    val ctt_code = evt.target.asInstanceOf[XMLHttpRequest].responseText
    println("reqListener")
    cttArea.value = ctt_code
    cttChanged(null)
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
      if (operators.contains(name)) return ""
      if (name.startsWith("show")
        || name.startsWith("check")
        || name.startsWith("is")) return "application.gif"
      return "interaction.gif"
    }

    override def toString = {
      name
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
    el.setAttribute("width", "" + (node.width + 32))
    el.setAttribute("height", "1000")

    def render_recurse_lines(n: CttNode): Unit = {

      var prevChild: CttNode = null
      for (child <- n.children) {
        if (prevChild != null) {
          val line = document.createElementNS("http://www.w3.org/2000/svg", "line").asInstanceOf[SVGLineElement]
          line.setAttribute("x1", "" + (prevChild.pos.x + 16))
          line.setAttribute("y1", "" + (prevChild.pos.y))
          line.setAttribute("x2", "" + (child.pos.x - 16))
          line.setAttribute("y2", "" + child.pos.y)
          line.style.strokeWidth = "2"
          line.style.stroke = "rgb(100, 100, 100)"
          el.appendChild(line)
        }
        val icon = child.GetIconName()
        if(!isEmpty(icon))
        {
          val line = document.createElementNS("http://www.w3.org/2000/svg", "line").asInstanceOf[SVGLineElement]
          line.setAttribute("x1", "" + n.pos.x)
          line.setAttribute("y1", "" + (n.pos.y + 16))
          line.setAttribute("x2", "" + child.pos.x)
          line.setAttribute("y2", "" + (child.pos.y - 16))
          line.style.strokeWidth = "2"
          line.style.stroke = "rgba(100, 100, 100, 200)"
          el.appendChild(line)
        }
        render_recurse_lines(child)
        prevChild = child
      }
    }

    def render_recurse(n: CttNode): Unit = {
      val text = document.createElementNS("http://www.w3.org/2000/svg", "text").asInstanceOf[SVGTextElement]
      text.setAttribute("x", "" + (n.pos.x - n.name.length * 3))
      text.setAttribute("width", "" + (n.name.length * 7.2))
      text.setAttribute("height", "15")
      text.innerHTML = n.name
      text.style.fontFamily = "monospace"

      val text_bg = document.createElementNS("http://www.w3.org/2000/svg", "rect").asInstanceOf[SVGRectElement]
      text_bg.setAttribute("x", text.getAttribute("x"))
      text_bg.setAttribute("width", text.getAttribute("width"))
      text_bg.setAttribute("height", text.getAttribute("height"))
      text_bg.style.fill = "rgba(255, 255, 255, 0.7)"

      val icon = n.GetIconName()
      if (icon == "") {
        text.setAttribute("y", "" + (n.pos.y + 2))
        text_bg.setAttribute("y", "" + (n.pos.y + 2 - 11))

        val rect = document.createElementNS("http://www.w3.org/2000/svg", "rect")
        rect.setAttribute("x", "" + (n.pos.x - 16))
        rect.setAttribute("y", "" + (n.pos.y - 16))
        rect.setAttribute("width", "32")
        rect.setAttribute("height", "32")
        rect.setAttribute("fill", "#FFFFFF") // to hide lines passing behind it
        el.appendChild(rect)
      } else {
        text.setAttribute("y", "" + (n.pos.y + 26))
        text_bg.setAttribute("y", "" + (n.pos.y + 26 - 11))

        val img = document.createElementNS("http://www.w3.org/2000/svg", "image").asInstanceOf[SVGImageElement]
        img.setAttribute("x", "" + (n.pos.x - 16))
        img.setAttribute("y", "" + (n.pos.y - 16))
        img.setAttribute("width", "32")
        img.setAttribute("height", "32")
        img.setAttributeNS("http://www.w3.org/1999/xlink", "href", icon)
        img.setAttributeNS(null, "visibility", "visible")
        el.appendChild(img)
      }

      el.appendChild(text_bg)
      el.appendChild(text)

      for (child <- n.children) {
        render_recurse(child)
      }
    }

    render_recurse_lines(node)
    render_recurse(node)
    return el
  }

  def isEmpty(x: String): Boolean = x == null || x.isEmpty

  def linear_parse_ctt(code: String): CttNode = {

    var ctt_code = "\n" + code.trim + "\n"
    ctt_code = ctt_code.replace("\r", "")


    val root = new CttNode
    root.name = "standard_root_node"
    val stack = new Stack[CttNode]

    var currentNode = root

    var indentLevel: Int = -1

    var currentCharIndex: Int = 0
    var nextCharIndex = 0 // ignore first itteration.

    while (nextCharIndex != -1) {
      val line = ctt_code.substring(currentCharIndex, nextCharIndex)
      if (!isEmpty(line)) {
        println(line)
        val leading_tabs = count_leading_tabs(line)
        var node = new CttNode
        node.name = line.substring(leading_tabs)
        if (leading_tabs == indentLevel + 1) {
          stack.push(currentNode)
        } else if (leading_tabs == indentLevel) {
          // nothing special to do
        } else if (leading_tabs < indentLevel) {
          shrink_stack(stack, leading_tabs + 1)
        } else {
          throw new Exception("Something went wrong")
        }

        indentLevel = leading_tabs
        currentNode = node
        stack.top.children += node

      }
      currentCharIndex = nextCharIndex + 1
      nextCharIndex = ctt_code.indexOf("\n", currentCharIndex + 1) // seek after newline
    }

    return root
  }

  def shrink_stack(stack: Stack[CttNode], size: Int): Unit = {
    var stackSize = stack.size
    assert(stackSize >= size)
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
