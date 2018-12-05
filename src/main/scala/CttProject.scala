import java.io.File

import scala.collection.immutable

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
    var f = getListOfFiles(projectPath)
    return f.filter(x => x.getName.endsWith(".txt")).map(x => x.getName)
  }
}
