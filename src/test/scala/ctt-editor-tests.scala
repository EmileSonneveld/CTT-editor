
import org.scalatest.FunSuite
import main.scala.CttEditor.CttNode
import main.scala.CttEditor
import scala.collection.mutable
import scala.io.Source

class CttEditorTest extends FunSuite {
  test("CttEditor.count_leading_tabs") {
    assert(CttEditor.count_leading_tabs("lol") === 0)
    assert(CttEditor.count_leading_tabs("\tlol") === 1)
    assert(CttEditor.count_leading_tabs("\t\tlol") === 2)
    assert(CttEditor.count_leading_tabs("\t\tlol\t\t\t\t") === 2)
  }

  test("CttEditor.shrink_stack") {
    val stack = new mutable.Stack[CttNode]()
    assert(stack.size === 0)
    stack.push(new CttNode)
    assert(stack.size === 1)
    stack.push(new CttNode)
    stack.push(new CttNode)
    stack.push(new CttNode)
    assert(stack.size === 4)
    CttEditor.shrink_stack(stack, 2)
    assert(stack.size === 2)
  }

  test("CttEditor.linear_parse_ctt") {
    val fileContents = Source.fromFile("www/example.txt").getLines.mkString("\n")
    val ctt = CttEditor.linear_parse_ctt(fileContents)
    CttEditor.calculateWidth(ctt)
    CttEditor.calculatePosition(ctt)

    val serialise = CttEditor.print_ctt(ctt.children(0))
    assert(fileContents.trim === serialise.trim)
  }


  test("trimming") {
    assert("\n\n\nXXX\n\n\n".trim === "XXX")
  }

}
