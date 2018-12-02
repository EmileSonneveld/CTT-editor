package tutorial.webapp

//import java.util

import org.scalajs.dom
import dom.document

import scala.collection.mutable.ListBuffer
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.control.Breaks._
import scala.collection.mutable.Stack

object TutorialApp {

  val simple_ctt =
    """
root_node
	child_1
	child_2
    """.stripMargin
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
	quit"""

  def main(args: Array[String]): Unit = {
    //println(ctt_code)
    //appendPar(document.body, "Hello World. Emile2")

    var simple = linear_parse_ctt(simple_ctt.replace("\r", ""))
    println(simple)
  }

  class CttNode {
    var name = "untitled_node"
    var children= ListBuffer[CttNode]()
  }

  def isEmpty(x: String) = x == null || x.isEmpty

  def linear_parse_ctt(code: String): CttNode = {
    val root = new CttNode
    root.name = "standard_root_node"
    val stack = new Stack[CttNode]
    stack.push(root)

    var indentLevel: Int = 0

    var currentCharIndex: Int = 0
    var nextCharIndex = 0 // ignore first itteration.

    while (nextCharIndex != -1) {
      val line = code.substring(currentCharIndex, nextCharIndex)
      if (!isEmpty(line)) {
        var leading_tabs = count_leading_tabs(line)
        var node = new CttNode
        node.name = line.substring(leading_tabs)
        if (leading_tabs == indentLevel) {
          stack.top.children += node
        } else if (leading_tabs > indentLevel) {
          if (leading_tabs > indentLevel + 1) throw new Exception("Too much indentation")
          indentLevel = leading_tabs
          stack.push(node)
        } else { // De-indent
          shrink_stack(stack, leading_tabs + 1)
          stack.push(node)
        }
      }
      currentCharIndex = nextCharIndex
      nextCharIndex = code.indexOf("\n", currentCharIndex + 1)
    }

    return root
  }

  def shrink_stack(stack:Stack[CttNode], size:Int) = {
    assert(stack.size >=size)
    for(i <- 0 until stack.size - size){
      stack.pop()
    }
  }

/*
  def parse_ctt(code: String): CttNode = {
    var currentCharIndex: Int = 0
    var indentLevel: Int = 0

    def parse_recusive(): CttNode = {
      val root = new CttNode()

      currentCharIndex = 0
      var nextCharIndex = code.indexOf("\n", currentCharIndex)
      while (nextCharIndex != -1) {
        var line = code.substring(currentCharIndex, nextCharIndex)

        if (!isEmpty(line)) {
          var leading_tabs = count_leading_tabs(line)
          if (leading_tabs == indentLevel) {
            if (!isEmpty(root.name)) return root // we found a sibling
            root.name = line.substring(leading_tabs)
          } else if (leading_tabs < indentLevel) {
            break
          } else {
            indentLevel += 1
            var child = parse_recusive()
            indentLevel -= 1
            root.children += child
          }
        }

        currentCharIndex = nextCharIndex
        nextCharIndex = code.indexOf("\n", currentCharIndex)
      }
      return root
    }

    return parse_recusive()
  }*/

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
