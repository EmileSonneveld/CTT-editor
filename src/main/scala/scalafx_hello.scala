import javafx.collections.ObservableList
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.ListView
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout._
import scalafx.scene.paint.Color._
import scalafx.scene.paint.Stops
import scalafx.scene.text.Text
import scalafx.scene.web.WebView

import scala.io.Source

object ScalaFXHelloWorld extends JFXApp {

  private val wv = new WebView {}
  private val fileContents = Source.fromFile("www/example.svg").getLines.mkString("\n")
  wv.engine.loadContent(fileContents, "image/svg+xml")

  private val project = new CttProject
  project.OpenProjectFromFolder("C:\\Users\\emill\\Dropbox\\slimmerWorden\\2018-2019-Semester1\\CMDM\\PROJECT\\ctt-editor-files")
  var f = project.getCttFiles()


  private val listView = new ListView[String]()
  val bf = new ObservableBuffer[String]()
  f.copyToBuffer(bf)
  listView.items = bf

  def selectedFileChanged = {
    println("selectedFileChanged: " + listView.selectionModel.value)
  }

  listView.selectionModel.onChange(selectedFileChanged)

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
              new Text {
                text = "editor "
                style = "-fx-font-size: 48pt"
                style = "-fx-background-color: #AAAAAA"
                hgrow = Priority.Always

                hgrow = Priority.Always
                fill = Gray
              }
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
