package main.scala


import scala.collection.mutable.{ListBuffer, Stack}


class Vector2D(_x: Double, _y: Double) {
  var x: Double = _x
  var y: Double = _y
}


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

class EnabledTaskSet {
  var tasks = ListBuffer[CttNode]()

  override def toString: String = {
    "{" + tasks.mkString(", ") + "}"
  }
}

class EnabledTaskSets {
  var sets = ListBuffer[EnabledTaskSet]()

  override def toString: String = {
    sets.mkString("\n")
  }
}







class StaticUtil {

  def ctt_to_enabled_task_sets(ctt: CttNode): EnabledTaskSets = {
    var etss = new EnabledTaskSets();
    {
      val ets = new EnabledTaskSet();
      ets.tasks += ctt
      etss.sets += ets
    }

    def level_pass() = {
      val etss_len = etss.sets.length // Don't loop over the new ETSes
      for (i <- 0 until etss_len) {
        val ets = etss.sets(i)
        println("Enabled task set")
        var j = 0
        while (j < ets.tasks.length) {
          val task = ets.tasks(j)
          println("  task: " + task + "  j: " + j)

          if (task.children.length > 0) {
            ets.tasks.remove(j)

            var lastOperator = ""
            for (child <- task.children) {
              if (child.IsOpperator())
                lastOperator = child.name
              else {
                if (lastOperator == "" // Still the first element / head. This is always added
                  || lastOperator == "[]"
                ) {
                  ets.tasks.insert(j, child)
                  j += 1
                } else if(lastOperator == ">>"
                  || lastOperator == "[]>>"){

                  val ets_new = new EnabledTaskSet()
                  ets_new.tasks += ctt
                  etss.sets += ets
                }
              }
            }
          }
          j += 1
        }
      }
    }

    level_pass()
    return etss
  }


  def ctt_code_to_svg(cttCode: String): String = {
    val ctt = linear_parse_ctt(cttCode)
    calculateWidth(ctt)
    calculatePosition(ctt)
    val svg = render_ctt_to_svg(ctt.children(0))
    return svg
  }


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
  def isEmpty(x: String): Boolean = x == null || x.isEmpty


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
}
