package tutorial.webapp

//import java.util

import org.scalajs.dom
import dom.document

import scala.collection.mutable.ListBuffer
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.control.Breaks._
import scala.collection.mutable.Stack

object CttEditor {

  val simple_ctt =
    """
trunk_node
	child_1
	child_2
    """
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

    var simple = linear_parse_ctt(ctt_code.replace("\r", ""))
    val str = print_ctt(simple)
    println(str)
    //println(simple)
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

  def shrink_stack(stack:Stack[CttNode], size:Int) = {
    assert(stack.size >=size)
    for(i <- 0 until stack.size - size){
      stack.pop()
    }
  }

  def print_ctt(node:CttNode): String =
  {
    val sb = new StringBuilder

    def print_recurse(n:CttNode, indentLevel:Int = 0):Unit = {
      sb.append(("\t" * indentLevel) + n.name+"\n")
      for(child <- n.children){
        print_recurse(child, indentLevel+1)
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
