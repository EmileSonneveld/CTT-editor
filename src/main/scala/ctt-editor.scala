package main.scala

import org.scalajs.dom
import dom.{Event, XMLHttpRequest, document}
import org.scalajs.dom.raw._

import scala.collection.mutable.ListBuffer
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.control.Breaks._
import scala.collection.mutable.Stack
import scala.scalajs.js
import scala.scalajs.js.{JSON, URIUtils}
import play.api.libs.json._

class Vector2D(_x: Double, _y: Double) {
  var x: Double = _x
  var y: Double = _y
}


object CttEditor {

  var cttArea: HTMLTextAreaElement = dom.document.body.querySelector("#ctt-texarea").asInstanceOf[HTMLTextAreaElement]
  var cttHolder: Element = dom.document.body.querySelector("#ctt-holder")
  var cttFiles: HTMLSelectElement = dom.document.body.querySelector("#ctt-files").asInstanceOf[HTMLSelectElement]
  var cttFilter: HTMLInputElement = dom.document.body.querySelector("#ctt-filter").asInstanceOf[HTMLInputElement]
  var cttMake: Element = dom.document.body.querySelector("#ctt-make")


  def main(args: Array[String]): Unit = {
    println(args.mkString(", "))
    //if(dom.document == null) return

    cttFilter.addEventListener("change", cttFilterChanged)
    cttFilter.addEventListener("keyup", cttFilterChanged)

    cttMake.addEventListener("click", makeNewCtt)

    cttArea.addEventListener("change", cttChanged)
    cttArea.addEventListener("keyup", cttChanged)

    cttFiles.addEventListener("change", selectedFileChanged)

    loadFileList()

    cttFiles.selectedIndex = 0 // doesn't trigger the on change
    selectedFileChanged(null)
  }

  private def loadFileList():Unit = {
    val oReq = new XMLHttpRequest()
    oReq.addEventListener("load", gotFileNames)
    oReq.open("GET", "../ctt-editor-files/", async = false)
    oReq.send()
  }

  private def gotFileNames(evt: Event): Unit = {
    val files = evt.target.asInstanceOf[XMLHttpRequest].responseText
    val json = Json.parse(files).as[List[JsValue]]

    val innerHtml = json.map(x => {
      val nameOrig = "" + x("name").asInstanceOf[JsString].value
      var name = nameOrig
      if (name.endsWith(".txt")) name = name.substring(0, name.length - ".txt".length)
      s"<option value='${nameOrig}'>${name}</option>"
    }).mkString("\n")
    cttFiles.innerHTML = innerHtml
  }

  private def cttFilterChanged(evt:Event) = {
    val filterValue = cttFilter.value
    var children = cttFiles.children
    var atLeastOneVisible = false
    for(i<- 0 until children.length)
    //for (var i = 0; i < children.length; i+=1)
    {
      var child = children(i).asInstanceOf[HTMLOptionElement]
      if(child.value.contains(filterValue)){
        child.removeAttribute("hidden")
        atLeastOneVisible = true
      }
      else
        child.setAttribute("hidden", "")
    }

    if(atLeastOneVisible)
      cttMake.setAttribute("hidden", "")
    else
      cttMake.removeAttribute("hidden")
  }

  private def makeNewCtt(evt:Event):Unit = {
    var newCttName = cttFilter.value
    if(!newCttName.endsWith(".txt"))
      newCttName+=".txt"

    {
      val oReq = new XMLHttpRequest()
      //oReq.addEventListener("load", fileUploaded)
      oReq.open("POST", "../ctt-editor-files/" + newCttName, async = false)
      oReq.setRequestHeader("file_content", URIUtils.encodeURI("")) // empty file
      oReq.send()
    }
    loadFileList()
    cttFiles.value = newCttName
    cttArea.value = ""
  }

  private def selectedFileChanged(evt: Event): Unit = {
    val oReq = new XMLHttpRequest()
    oReq.addEventListener("load", gotNewFileContent)
    oReq.open("GET", "../ctt-editor-files/" + cttFiles.value) //, async = false)
    oReq.send()
  }

  def cttChanged(evt: Event): Unit = {

    val ctt_code = cttArea.value
    cttHolder.innerHTML = ctt_code_to_svg(ctt_code)

    val oReq = new XMLHttpRequest()
    oReq.addEventListener("load", fileUploaded)
    oReq.open("POST", "../ctt-editor-files/" + cttFiles.value) //, async = false)
    oReq.setRequestHeader("file_content", URIUtils.encodeURI(ctt_code))
    oReq.send()

    println("cttChanged and was valid")
  }

  def fileUploaded(evt: Event) = {

  }

  def ctt_code_to_svg(cttCode: String): String = {
    val ctt = linear_parse_ctt(cttCode)
    calculateWidth(ctt)
    calculatePosition(ctt)
    val svg = render_ctt_to_svg(ctt.children(0))
    return svg
  }

  def gotNewFileContent(evt: Event): Unit = {
    val ctt_code = evt.target.asInstanceOf[XMLHttpRequest].responseText
    println("gotNewFileContent")
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

  def render_ctt_to_svg(node: CttNode): String = {
    val sb = new StringBuilder
    sb.append("<?xml version='1.0' encoding='UTF-8' ?>\n")
    sb.append("<svg width='" + (node.width + 32) + "' height='1000' xmlns='http://www.w3.org/2000/svg' version='1.1'>\n")

    def render_recurse_lines(n: CttNode): Unit = {

      var prevChild: CttNode = null
      for (child <- n.children) {
        if (prevChild != null) {
          sb.append("<line x1='" + (prevChild.pos.x + 17) + "' y1='" + (prevChild.pos.y + 0.5) + "' x2='" + (child.pos.x - 17) + "' y2='" + (prevChild.pos.y + 0.5) + "' style='stroke-width: 1; stroke: rgb(10, 10, 10);'></line>\n")
        }
        val icon = child.GetIconName()
        if (!isEmpty(icon)) {
          sb.append("<line x1='" + (n.pos.x) + "' y1='" + (n.pos.y + 16) + "' x2='" + child.pos.x + "' y2='" + (child.pos.y - 16) + "' style='stroke-width: 1; stroke: rgba(10, 10, 10, 200);'></line>\n")
        }
        render_recurse_lines(child)
        prevChild = child
      }
    }

    def render_recurse(n: CttNode): Unit = {
      var text_y: Double = -1
      var bg_y: Double = -1
      val icon = n.GetIconName()
      if (icon == "") {
        text_y = (n.pos.y + 4)
        bg_y = (n.pos.y + 4 - 11)
        sb.append("<rect x='" + (n.pos.x - 16) + "' y='" + (n.pos.y - 16) + "' width='32' height='32' style='fill: #FFFFFF;'></rect>\n")

      } else {
        text_y = (n.pos.y + 26)
        bg_y = (n.pos.y + 26 - 11)
        sb.append("<image x='" + (n.pos.x - 16) + "' y='" + (n.pos.y - 16) + "' width='32' height='32' href='" + icon + "' visibility='visible'></image>\n")
      }

      val w = (n.name.length * 7.2)
      sb.append("<rect x='" + (n.pos.x - n.name.length * 3) + "' y='" + bg_y + "' width='" + w + "' height='15' style='fill: rgba(255, 255, 255, 0.7);'></rect>\n")
      sb.append("<text x='" + (n.pos.x - n.name.length * 3) + "' y='" + text_y + "' width='" + w + "' height='15' style='font-family: monospace;'>" + n.name + "</text>\n")


      for (child <- n.children) {
        render_recurse(child)
      }
    }

    render_recurse_lines(node)
    render_recurse(node)
    sb.append("</svg>\n")
    return sb.toString()
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
