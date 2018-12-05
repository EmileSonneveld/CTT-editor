import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.ObservableList
import javafx.scene.control.SelectionMode
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
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
  private val fileContents = Source.fromFile("www/example.svg").getLines.mkString("\n")
  wv.engine.loadContent(fileContents, "image/svg+xml")
  wv.maxWidth = 99999

  private val project = new CttProject
  //project.OpenProjectFromFolder("C:\\Users\\emill\\Dropbox\\slimmerWorden\\2018-2019-Semester1\\CMDM\\PROJECT\\ctt-editor-files")
  project.OpenProjectFromFolder("C:\\Users\\emill\\Dropbox (Persoonlijk)\\slimmerWorden\\2018-2019-Semester1\\CMDM\\PROJECT\\ctt-editor-files")
  private val f = project.getCttFiles()

  val textView = new TextArea()
  textView.vgrow = Priority.Always
  textView.textProperty.addListener(new InvalidationListener {
    override def invalidated(observable: Observable): Unit = onTextChanged()
  })
  def onTextChanged():Unit = {
    // TODO: Should only save when parsing was succesfull or when user explicitly asks
    //project.saveCttCode(listView.selectionModel.value.getSelectedItem, textView.textProperty.getValue)
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

  stage = new PrimaryStage {
    title = "ScalaFX Hello World"
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
              textView
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
