package main.scala

import scala.collection.mutable.{ListBuffer, Stack}


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


object StaticUtil {

  def escapeXml(unsafe: String): String = {
    return unsafe
      .replace("&", "&amp;") // Should be first
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\'", "&apos;")
      .replace("\"", "&quot;")
  }

  def normalise_ctt(ctt: CttNode): Unit = {

    def rec(n: CttNode): Unit = {

      var prio = 1
      while (prio <= CttNode.maxCttPriority) {
        var didSomething = false
        if (n.children.length > 3) {
          var i = 1 // Only loop odd indexes, they will contain the operators
          while (i < n.children.length) {
            val child = n.children(i)
            val op = child.Operator()
            assert(op != null, "Missing operator somewhere?")

            if (op.priority == prio) {
              didSomething = true
              val left = n.children(i - 1)
              val right = n.children(i + 1)
              val newNode = new CttNode
              n.children.remove(i + 1)
              n.children.remove(i)
              n.children.remove(i - 1)

              newNode.name = "(" + left.displayName + " & " + right.displayName + ")"
              newNode.addChild(left)
              newNode.addChild(child)
              newNode.addChild(right)
              n.addChild(newNode, i - 1)
            }
            i += 2
          }
        }
        if (!didSomething) prio += 1
      }
      for (child <- n.children)
        rec(child)
    }

    rec(ctt)
  }


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
        var j = 0
        while (j < ets.tasks.length) {
          val task = ets.tasks(j)

          if (task.children.length > 0) {
            ets.tasks.remove(j)

            var lastOperator = ""
            for (child <- task.children) {
              if (child.Operator() != null)
                lastOperator = child.Operator().name
              else {
                if (child.GetIconName() != "user.gif"
                  //|| child.GetIconName() == "abstraction.gif"
                  //|| child.name.toLowerCase().startsWith("show") // Todo: How to know when to show an application task or not?
                )
                {
                  if (lastOperator == "" // Still the first element / head. This is always added
                    || lastOperator == "[]"
                    || lastOperator == "|=|"
                    || lastOperator == "||"
                    || lastOperator == "|||"
                    || lastOperator == "|[]|"
                    || lastOperator == "[>" // Todo: Investigate further
                    || lastOperator == "|>" // Todo: Investigate further
                  ) {
                    ets.tasks.insert(j, child)
                    j += 1
                  } else if (lastOperator == ">>"
                    || lastOperator == "[]>>"
                  ) {
                    val ets_new = new EnabledTaskSet()

                    val enabledTasksLeftUp = child.findTaskLeftUp()
                    ets_new.tasks = enabledTasksLeftUp ++ ets_new.tasks // Second part will be empty, but just for good measure

                    ets_new.tasks += child

                    val enabledTasksRightUp = child.findTaskRightUp()
                    for (t <- enabledTasksRightUp)
                      ets_new.tasks += t

                    //if (desactivationTask != null)
                    //  ets_new.tasks += desactivationTask

                    etss.sets += ets_new
                  }
                }
              }
            }
            j -= 1 // We would oveshoot a bit at the end of this loop, because we removed a task in the beginning
          }
          j += 1
        }
      }
    }

    // Todo: Know when to stop with the loop
    for(_ <- 0 until 100) {
      level_pass()
    }
    return etss
  }


  def ctt_code_to_svg(ctt: CttNode): String = {
    calculateWidth(ctt)
    calculatePosition(ctt)
    val svg = render_ctt_to_svg(ctt)
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
          throw new Exception("Something went wrong with the indentation.")
        }

        indentLevel = leading_tabs
        currentNode = node
        stack.top.addChild(node)

      }
      currentCharIndex = nextCharIndex + 1
      nextCharIndex = ctt_code.indexOf("\n", currentCharIndex + 1) // seek after newline
    }

    var firstCtt = root.children(0) // We could support multiple CTTs per file.
    firstCtt.parent = null
    return firstCtt
  }

  def shrink_stack(stack: Stack[CttNode], size: Int): Unit = {
    var stackSize = stack.size
    assert(stackSize >= size, "Some indentation might be wrong")
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

    var lowest_y: Double = 0

    def get_lowest_y(n: CttNode): Unit = {
      if (n.pos.y > lowest_y)
        lowest_y = n.pos.y
      for (child <- n.children) {
        get_lowest_y(child)
      }
    }

    get_lowest_y(node)
    lowest_y += 26 + 15 // Node uses center position. Compencate for text label.

    val sb = new StringBuilder
    sb.append("<?xml version='1.0' encoding='UTF-8' ?>\n")
    sb.append("<svg width='" + (node.width + 32) + "' height='" + lowest_y + "' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' version='1.1'>\n")

    def render_recurse_lines(n: CttNode): Unit = {

      var prevChild: CttNode = null
      for (child <- n.children) {
        if (prevChild != null) {
          sb.append("<line x1='" + (prevChild.pos.x + 17) + "' y1='" + (prevChild.pos.y + 0.5) + "' x2='" + (child.pos.x - 17) + "' y2='" + (prevChild.pos.y + 0.5) + "' style='stroke-width: 1; stroke:#0a0a0a'></line>\n")
        }
        val icon = child.GetIconName()
        if (!isEmpty(icon)) {
          sb.append("<line x1='" + (n.pos.x) + "' y1='" + (n.pos.y + 16) + "' x2='" + child.pos.x + "' y2='" + (child.pos.y - 16) + "' style='stroke-width: 1; stroke:#0a0a0a; fill-opacity:0.7;'></line>\n")
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
        sb.append("<rect x='" + (n.pos.x - 16) + "' y='" + (n.pos.y - 16) + "' width='32' height='32' style='fill:#FFFFFF; fill-opacity:0.7;'></rect>\n")

      } else {
        text_y = (n.pos.y + 26)
        bg_y = (n.pos.y + 26 - 11)
        sb.append("<image x='" + (n.pos.x - 16) + "' y='" + (n.pos.y - 16) + "' width='32' height='32' xlink:href='" + icon + "' visibility='visible'></image>\n")
      }
      var nam = n.displayName()
      val nam_len = nam.length
      val w = (nam_len * 7.2)
      nam = escapeXml(nam)
      sb.append("<rect x='" + (n.pos.x - nam_len * 3) + "' y='" + bg_y + "' width='" + w + "' height='15' style='fill:#FFFFFF; fill-opacity:0.7;'></rect>\n")
      sb.append("<text x='" + (n.pos.x - nam_len * 3) + "' y='" + text_y + "' width='" + w + "' height='15' style='font-family: monospace;'>" + nam + "</text>\n")


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


  def calculatePosition(node: CttNode, offset: Vector2D = new Vector2D(0, 17)): Unit = {
    val sizePerLayer = 60

    node.pos.x = offset.x + node.width / 2
    node.pos.y = offset.y

    for (child <- node.children) {
      calculatePosition(child, Vector2D.add(offset, new Vector2D(0, sizePerLayer)))
      offset.x += child.width
    }
  }
}
