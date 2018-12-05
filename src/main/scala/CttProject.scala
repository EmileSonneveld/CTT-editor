package main.scala

import java.io.{File, PrintWriter}
import java.nio.file.Paths

import scala.io.Source


class CttProject {

  var projectPath: String = null

  private def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def OpenProjectFromFolder(path: String) = {
    projectPath = path
  }


  def getCttFiles(): List[String] = {
    val f = getListOfFiles(projectPath)
    return f.filter(x => x.getName.endsWith(".txt")).map(x => x.getName)
  }

  def getCttCode(cttFileName: String): String = {
    val p = Paths.get(projectPath, cttFileName).toString
    val fileContents = Source.fromFile(p).getLines.mkString("\n")
    return fileContents
  }

  // Will eat error if fails
  // Returns the path where the file is saved
  def saveFile(fileName: String, fileContent:String):String = {
    val p = Paths.get(projectPath, fileName).toString
    new PrintWriter(p) { try {write(fileContent)} finally {close} }
    return p
  }
}
