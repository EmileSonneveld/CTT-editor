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
  var cttSvgDownload: Element = dom.document.body.querySelector("#ctt-svg-download")
  var cttPngDownload: Element = dom.document.body.querySelector("#ctt-png-download")

  def main(args: Array[String]): Unit = {
    println(args.mkString(", "))
    //if(dom.document == null) return

    cttSvgDownload.addEventListener("click", cttSvgDownloadClicked)
    cttPngDownload.addEventListener("click", cttPngDownloadClicked)

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

  def downloadSvg(filename: String, text: String) {
    var element = document.createElement("a").asInstanceOf[HTMLLinkElement]
    element.setAttribute("href", "data:image/svg+xml;charset=utf-8," + URIUtils.encodeURIComponent(text))
    element.setAttribute("download", filename)
    element.style.display = "none"
    document.body.appendChild(element)
    element.click()
    document.body.removeChild(element)
  }

  private def cttPngDownloadClicked(evt: Event) = {
    scala.scalajs.js.eval("downloadSvgAsImage()") // can be cleaner, if function would be translated to Scala
  }

  private def cttSvgDownloadClicked(evt: Event) = {
    val newCttName = cttFiles.value + ".svg"
    val ctt = StaticUtil.linear_parse_ctt(this.cttArea.value)
    if (cttNormalize.checked)
      StaticUtil.normalise_ctt(ctt)
    val svg = StaticUtil.ctt_code_to_svg(ctt)
    downloadSvg(newCttName, svg)
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
      val emptyCtt = ""
      val oReq = new XMLHttpRequest()
      oReq.open("POST", "../ctt-editor-files/" + newCttName, async = false)
      oReq.setRequestHeader("file_content", URIUtils.encodeURI(emptyCtt)) // empty file
      oReq.send(emptyCtt)
    }
    loadFileList()
    cttFiles.value = newCttName
    cttArea.value = ""
  }

  private def selectedFileChanged(evt: Event): Unit = {
    val oReq = new XMLHttpRequest()
    oReq.addEventListener("load", gotNewFileContent)
    oReq.open("GET", "../ctt-editor-files/" + cttFiles.value, async = false)
    oReq.send()
  }

  var wantToUpladCtt = false
  var cttUploadingInProccess = false
  var cttHandlingError: String = ""

  def cttChanged(evt: Event): Unit = {
    try {
      cttHandlingError = ""
      ApplyErrorMessage()

      // Set UI state
      val ctt_code = cttArea.value
      val ctt = StaticUtil.linear_parse_ctt(ctt_code)
      if (cttNormalize.checked)
        StaticUtil.normalise_ctt(ctt)
      cttHolder.innerHTML = StaticUtil.ctt_code_to_svg(ctt)
      if (!cttNormalize.checked)
        StaticUtil.normalise_ctt(ctt)
      cttEts.innerHTML = StaticUtil.ctt_to_enabled_task_sets(ctt).toString.replace("\n", "<br/>\n")


      wantToUpladCtt = true
      cttUploadingBeatingHearth()
    } catch {
      case (e: Throwable) => {
        cttHandlingError = e.getMessage
        ApplyErrorMessage()
      }
    }
  }

  def ApplyErrorMessage(): Unit = {
    cttMessage.innerHTML = cttHandlingError + uploadError
  }

  var uploadError = ""

  def cttUploadingBeatingHearth() = {
    if (wantToUpladCtt) {
      if (cttUploadingInProccess) {
        // Wait to next 'hearthbeat'
      } else {
        try {
          uploadError = ""
          ApplyErrorMessage()

          // Set UI state
          val ctt_code = cttArea.value
          val ctt = StaticUtil.linear_parse_ctt(ctt_code)
          if (cttNormalize.checked)
            StaticUtil.normalise_ctt(ctt)
          cttHolder.innerHTML = StaticUtil.ctt_code_to_svg(ctt)
          if (!cttNormalize.checked)
            StaticUtil.normalise_ctt(ctt)
          cttEts.innerHTML = StaticUtil.ctt_to_enabled_task_sets(ctt).toString.replace("\n", "<br/>\n")


          val oReq = new XMLHttpRequest()
          oReq.addEventListener("error", fileUploadInterenetFailed)
          oReq.onreadystatechange = { (e: dom.Event) => {
            if (oReq.readyState == 4) {
              if (oReq.status >= 500) {
                uploadError = "Problem status: " + oReq.status + " responseText: " + oReq.responseText
              } else {
                uploadError = ""
              }
              ApplyErrorMessage()
              dom.window.setTimeout(() => fileUploaded_delayed(), 500) // Short delay to ba safe on slower backends
            }
          }
          }
          oReq.open("POST", "../ctt-editor-files/" + cttFiles.value) //, async = false)
          oReq.setRequestHeader("file_content", URIUtils.encodeURI(ctt_code))
          oReq.send(ctt_code)
          cttUploadingInProccess = true

          wantToUpladCtt = false
          println("cttChanged and was valid")
        } catch {
          case (e: Throwable) => {
            uploadError = e.getMessage
            ApplyErrorMessage()
          }
        }
      }
    }
  }

  def fileUploaded_delayed(): Unit = {
    cttUploadingInProccess = false
    cttUploadingBeatingHearth()
  }

  def fileUploadInterenetFailed(evt: Event) = {
    uploadError = "Internet connection problem."
    ApplyErrorMessage()
    dom.window.setTimeout(() => fileUploaded_delayed(), 500) // Short delay to ba safe on slower backends
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
