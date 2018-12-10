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


object CttEditor {

  var cttArea: HTMLTextAreaElement = dom.document.body.querySelector("#ctt-texarea").asInstanceOf[HTMLTextAreaElement]
  var cttHolder: Element = dom.document.body.querySelector("#ctt-holder")
  var cttFiles: HTMLSelectElement = dom.document.body.querySelector("#ctt-files").asInstanceOf[HTMLSelectElement]
  var cttFilter: HTMLInputElement = dom.document.body.querySelector("#ctt-filter").asInstanceOf[HTMLInputElement]
  var cttMake: Element = dom.document.body.querySelector("#ctt-make")
  var cttEts: HTMLDivElement = dom.document.body.querySelector("#ctt-ets").asInstanceOf[HTMLDivElement]
  var cttNormalize: HTMLInputElement = dom.document.body.querySelector("#ctt-normlize").asInstanceOf[HTMLInputElement]
  var cttMessage: HTMLInputElement = dom.document.body.querySelector("#ctt-message").asInstanceOf[HTMLInputElement]

  def main(args: Array[String]): Unit = {
    println(args.mkString(", "))
    //if(dom.document == null) return

    cttFilter.addEventListener("change", cttFilterChanged)
    cttFilter.addEventListener("keyup", cttFilterChanged)
    cttFilterChanged(null)

    cttMake.addEventListener("click", makeNewCtt)

    cttArea.addEventListener("change", cttChanged)
    cttArea.addEventListener("keyup", cttChanged)

    cttNormalize.addEventListener("change", cttChanged)

    cttFiles.addEventListener("change", selectedFileChanged)


    loadFileList()

    cttFiles.selectedIndex = 0 // doesn't trigger the on change
    selectedFileChanged(null)
  }

  private def loadFileList(): Unit = {
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

  private def cttFilterChanged(evt: Event) = {
    val filterValue = cttFilter.value
    var children = cttFiles.children
    var atLeastOneVisible = false
    for (i <- 0 until children.length)
    //for (var i = 0; i < children.length; i+=1)
    {
      var child = children(i).asInstanceOf[HTMLOptionElement]
      if (child.value.contains(filterValue)) {
        child.removeAttribute("hidden")
        atLeastOneVisible = true
      }
      else
        child.setAttribute("hidden", "")
    }

    if (atLeastOneVisible)
      cttMake.setAttribute("hidden", "")
    else
      cttMake.removeAttribute("hidden")
  }

  private def makeNewCtt(evt: Event): Unit = {
    var newCttName = cttFilter.value
    if (!newCttName.endsWith(".txt"))
      newCttName += ".txt"

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
    try {
      cttMessage.innerHTML = ""

      val ctt_code = cttArea.value
      val ctt = StaticUtil.linear_parse_ctt(ctt_code)
      if (cttNormalize.checked)
        StaticUtil.normalise_ctt(ctt)
      cttHolder.innerHTML = StaticUtil.ctt_code_to_svg(ctt)
      if (!cttNormalize.checked)
        StaticUtil.normalise_ctt(ctt)
      cttEts.innerHTML = StaticUtil.ctt_to_enabled_task_sets(ctt).toString.replace("\n", "<br/>\n")

      val oReq = new XMLHttpRequest()
      oReq.addEventListener("load", fileUploaded)
      oReq.addEventListener("error", fileUploadFailed);
      oReq.open("POST", "../ctt-editor-files/" + cttFiles.value) //, async = false)
      oReq.setRequestHeader("file_content", URIUtils.encodeURI(ctt_code))
      oReq.send()

      println("cttChanged and was valid")
    } catch {
      case (e: Throwable) => {
        cttMessage.innerHTML = e.getMessage
      }
    }
  }

  def fileUploaded(evt: Event) = {
    cttMessage.innerHTML = ""
  }

  def fileUploadFailed(evt: Event) = {
    if (dom.window.location.protocol == "file:")
      cttMessage.innerHTML = "CTT upload failed. <br/>This app should be accesed trough a webserver, not as a plain HTML-file."
    else
      cttMessage.innerHTML = "CTT upload failed. <br/>Submit issue here: https://github.com/EmileSonneveld/CTT-editor"
  }


  def gotNewFileContent(evt: Event): Unit = {
    val ctt_code = evt.target.asInstanceOf[XMLHttpRequest].responseText
    println("gotNewFileContent")
    cttArea.value = ctt_code
    cttChanged(null)
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
