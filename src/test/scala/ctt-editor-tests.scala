
import org.scalatest.FunSuite
import main.scala._

import scala.collection.mutable
import scala.io.Source

class CttEditorTest extends FunSuite {
  test("CttEditor.count_leading_tabs") {
    assert(StaticUtil.count_leading_tabs("lol") === 0)
    assert(StaticUtil.count_leading_tabs("\tlol") === 1)
    assert(StaticUtil.count_leading_tabs("\t\tlol") === 2)
    assert(StaticUtil.count_leading_tabs("\t\tlol\t\t\t\t") === 2)
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
    StaticUtil.shrink_stack(stack, 2)
    assert(stack.size === 2)
  }

  test("CttEditor.linear_parse_ctt") {
    val fileContents = Source.fromFile("www/example.txt").getLines.mkString("\n")
    val ctt = StaticUtil.linear_parse_ctt(fileContents)
    StaticUtil.calculateWidth(ctt)
    StaticUtil.calculatePosition(ctt)

    val serialise = StaticUtil.print_ctt(ctt)
    assert(fileContents.trim === serialise.trim)
  }


  test("trimming") {
    assert("\n\n\nXXX\n\n\n".trim === "XXX")
  }


  test("ets parsing") {
    val ctt_code = """Root
	Task 10
		Task 12
		>>
		Task 13
	[]
	Task 11
		Task 14
		[]>>
		Task 15"""
    val ctt = StaticUtil.linear_parse_ctt(ctt_code)
    println(StaticUtil.print_ctt(ctt))
    val etss = StaticUtil.ctt_to_enabled_task_sets(ctt)
    println(etss)
    assert(clean("""{Task 12, Task 14}
{Task 13}
{Task 15}""") === clean(etss.toString))
  }

  def clean(str:String):String = {
    str.replace("\r", "").trim.toLowerCase()
  }

  test("normalize") {
    val ctt_code = """Root
	Task 1
	>>
	Task 2
	[]
	Task 3"""
    val ctt = StaticUtil.linear_parse_ctt(ctt_code)
    println(StaticUtil.print_ctt(ctt))

    StaticUtil.normalise_ctt(ctt)

    println(StaticUtil.print_ctt(ctt))
  }

  test("forumETS") {

    val fileContents = Source.fromFile("ctt-editor-files/forum.txt").getLines.mkString("\n")
    val ctt = StaticUtil.linear_parse_ctt(fileContents)
    //println(StaticUtil.print_ctt(ctt))

    StaticUtil.normalise_ctt(ctt)
    var etss = StaticUtil.ctt_to_enabled_task_sets(ctt)
    val etssStr = etss.toString

    var shouldLookLikeThis = """{Show categories, select category, submit category, Quit}
      |{Show content category, Enter title, Enter content, Submit, select answer, Select comment ,Stop category, Quit}
      |{Show content category, Post on Forum, Stop category, Quit}
      |{Show content category, Enter response, Stop category, Quit}
      |{Show content category, save, Stop category, Quit}
      |{Show content category, Show, Stop category, Quit}"""
    println(etss)
  }
}
