package main.scala

import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.ObservableList
import javafx.scene.control.SelectionMode
import javax.swing.JPanel
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.embed.swing.SwingNode
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{ListView, TextArea, TextField}
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout._
import scalafx.scene.paint.Color._
import scalafx.scene.paint.Stops
import scalafx.scene.text.Text
import scalafx.scene.web.WebView

import scala.io.Source

object ScalafxGui extends JFXApp {

  private val wv = new WebView {}
  wv.maxWidth = 99999

  private val project = new CttProject
  //project.OpenProjectFromFolder("C:\\Users\\emill\\Dropbox\\slimmerWorden\\2018-2019-Semester1\\CMDM\\PROJECT\\ctt-editor-files")
  project.OpenProjectFromFolder("C:\\Users\\emill\\Dropbox (Persoonlijk)\\slimmerWorden\\2018-2019-Semester1\\CMDM\\PROJECT\\ctt-editor-files")
  private val f = project.getCttFiles()
  wv.engine.load("www/example.svg")

  val textView = new TextArea()
  textView.vgrow = Priority.Always
  textView.textProperty.addListener(new InvalidationListener {
    override def invalidated(observable: Observable): Unit = onTextChanged()
  })

  def wrapWithHtml(code:String):String = {
      """
<!DOCTYPE html>
<html>
<head>
</head>
<body>
      """ + code +
      """
</body>
</html>
      """
  }

  def onTextChanged(): Unit = {
    val ctt_code = textView.textProperty.getValue
    var svg = CttEditor.ctt_code_to_svg(ctt_code)
    svg = wrapWithHtml(svg)
    var path = project.saveFile("out/" + listView.selectionModel.value.getSelectedItem + ".html", svg)
    path  = "file:///" + path
    wv.engine.load("file:///C:/Users/emill/Dropbox%20(Persoonlijk)/slimmerWorden/2018-2019-Semester1/CMDM/PROJECT/ctt-editor-files/out/acces_schedule.txt.html")
    //wv.engine.loadContent(svg, "image/svg+xml")

    project.saveFile(listView.selectionModel.value.getSelectedItem, textView.textProperty.getValue)
  }

  private val listView = new ListView[String]()
  val bf = new ObservableBuffer[String]()
  f.copyToBuffer(bf)
  listView.items = bf

  def selectedFileChanged() = {
    println("selectedFileChanged: " + listView.selectionModel.value.getSelectedItem)
    textView.text = project.getCttCode(listView.selectionModel.value.getSelectedItem)
  }

  listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE)
  listView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener {
    override def invalidated(observable: Observable): Unit = selectedFileChanged()
  })

  {

    CefApp cefApp_ = CefApp.getInstance();
    CefClient client_ = cefApp_.createClient();
    CefBrowser browser_ = client_.createBrowser("https://www.google.com", OS.isLinux(), false);
    Component browerUI_ = browser_.getUIComponent(); JPanel panel = new JPanel(); panel.add(browerUI_);
    SwingNode swingNode = new SwingNode(); swingNode.setContent(panel);


  }

  stage = new PrimaryStage {
    title = "CTT-editor"
    scene = new Scene {
      fill = White
      minWidth = 600
      minHeight = 600

      root = new VBox {
        padding = Insets(5)
        prefWidth = 100
        prefHeight = 100
        style = "-fx-background-color: #FFAA55"

        children = Seq(
          new HBox {
            children = Seq(
              listView,
              textView,
              //svgShow,
            )
          },

          new HBox {
            style = "-fx-background-color: #003333"
            vgrow = Priority.Always

            children = Seq(
              wv
            )
          }
        )
      }
    }
  }
}
