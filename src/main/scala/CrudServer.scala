import java.net.InetSocketAddress
import java.io._
import java.util
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.nio.file.Paths
import java.net.URLDecoder
import java.awt.Desktop
import java.net.URI

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}


object CrudServer extends App {
  var server = HttpServer.create(new InetSocketAddress(6773), 0) // Port number is 'CTTE' written in bad 1337
  server.createContext("/", new StaticHandler())
  server.setExecutor(null) // creates a default executor
  server.start()
  println("Server has started. Open his url in a browser: http://localhost:6773/www/")

  if (Desktop.isDesktopSupported) Desktop.getDesktop.browse(new URI("http://localhost:6773/www/"))

  // Inspired from: https://github.com/ianopolous/simple-http-server/blob/master/src/http/StaticHandler.java
  class StaticHandler extends HttpHandler {

    private class Asset(val data: Array[Byte]) {
    }

    private val pathToRoot = ""
    private val caching = false
    private val gzip = false
    private val data = new util.HashMap[String, Asset]

    @throws[IOException]
    def handle(httpExchange: HttpExchange): Unit = {
      var path = httpExchange.getRequestURI.getPath
      if (path.contains("..")) throw new Exception("Supspicious path")
      path = path.substring(1)
      path = path.replaceAll("//", "/")

      var res: Asset = null
      var status = 500 // If not changed, we throw an error
      try {
        println(httpExchange.getRequestMethod + " " + httpExchange.getRequestURI.getPath)
        if (httpExchange.getRequestMethod == "POST") {

          // determine encoding// determine encoding

          //val parms = getPostParms(httpExchange.getRequestBody)
          //val fileContent = parms.get("file_content").get(0)
          val fileContent = URLDecoder.decode((httpExchange.getRequestHeaders().get("file_content")).get(0), "utf-8")

          val f = new File(pathToRoot + path)
          new PrintWriter(f) {try {write(fileContent)} finally {close}}
          res = new Asset("File written".getBytes())
          status = 200

        } else {
          if (new File(pathToRoot + path + "index.html").exists) path += "index.html"
          if (path.endsWith("/")) {
            httpExchange.getResponseHeaders.set("Content-Type", "application/json")
            val sb = new StringBuilder
            sb.append("[\n")
            sb.append(getListOfFiles(pathToRoot + path)
              .map(f => s"""{\n\t"name":"${f.getName}"\n}""")
              .mkString(",\n"))
            sb.append("]\n")
            res = new Asset(sb.toString().getBytes)
          } else {
            val fromFile = new File(pathToRoot + path).exists
            val in = if (fromFile) new FileInputStream(pathToRoot + path)
            else ClassLoader.getSystemClassLoader.getResourceAsStream(pathToRoot + path)
            res = if (caching) data.get(path)
            else new Asset(readResource(in, gzip))
            if (gzip) httpExchange.getResponseHeaders.set("Content-Encoding", "gzip")
            if (path.endsWith(".js")) httpExchange.getResponseHeaders.set("Content-Type", "text/javascript")
            else if (path.endsWith(".html")) httpExchange.getResponseHeaders.set("Content-Type", "text/html")
            else if (path.endsWith(".css")) httpExchange.getResponseHeaders.set("Content-Type", "text/css")
            else if (path.endsWith(".json")) httpExchange.getResponseHeaders.set("Content-Type", "application/json")
            else if (path.endsWith(".svg")) httpExchange.getResponseHeaders.set("Content-Type", "image/svg+xml")
          }
          status = 200

        }
      } catch {
        case t: NullPointerException =>
          System.err.println("Error retrieving: " + path)
          status = 500
          res = new Asset("NullPointerException".getBytes())
        case t: Throwable =>
          System.err.println("Error retrieving: " + path)
          t.printStackTrace()
          status = 500
          res = new Asset(t.getMessage.getBytes())
      }

      if (httpExchange.getRequestMethod.equals("HEAD")) {
        httpExchange.getResponseHeaders.set("Content-Length", "" + res.data.length)
        httpExchange.sendResponseHeaders(status, -1)
        return
      }
      httpExchange.sendResponseHeaders(status, res.data.length)
      httpExchange.getResponseBody.write(res.data)
      httpExchange.getResponseBody.close()
    }

    def getPostParms(requestBody: InputStream): util.HashMap[String, util.ArrayList[String]] = {

      //val reqHeaders = httpExchange.getRequestHeaders
      //val contentType = reqHeaders.getFirst("Content-Type")
      var encoding = "ISO-8859-1"
      //if (contentType != null) {
      //  val parms = ValueParser.parse(contentType)
      //  if (parms.containsKey("charset")) encoding = parms.get("charset")
      //}


      // read the query string from the request body
      var qry: String = null
      val in = requestBody
      try {
        val out = new ByteArrayOutputStream
        val buf = new Array[Byte](4096)
        var n = in.read(buf)
        while ( {
          n > 0
        }) {
          out.write(buf, 0, n)

          n = in.read(buf)
        }
        qry = new String(out.toByteArray, encoding)
      } finally in.close
      // parse the query
      val parms = new util.HashMap[String, util.ArrayList[String]]()
      val defs = qry.split("[&]")
      for (defi <- defs) {
        val ix = defi.indexOf('=')
        var name: String = null
        var value: String = null
        if (ix < 0) {
          name = URLDecoder.decode(defi, encoding)
          value = ""
        }
        else {
          name = URLDecoder.decode(defi.substring(0, ix), encoding)
          value = URLDecoder.decode(defi.substring(ix + 1), encoding)
        }
        var list = parms.get(name)
        if (list == null) {
          list = new util.ArrayList[String]
          parms.put(name, list)
        }
        list.add(value)

      }
      return parms
    }

    def getListOfFiles(dir: String): List[File] = {
      val d = new File(dir)
      if (d.exists && d.isDirectory) {
        d.listFiles.filter(_.isFile).toList
      } else {
        List[File]()
      }
    }

    @throws[IOException]
    private def processFile(path: String, f: File, gzip: Boolean): Unit = {
      if (!f.isDirectory) data.put(path + f.getName, new Asset(readResource(new FileInputStream(f), gzip)))
      if (f.isDirectory) {
        for (sub <- f.listFiles) {
          processFile(path + f.getName + "/", sub, gzip)
        }
      }
    }

    @throws[IOException]
    private def readResource(in: InputStream, gzip: Boolean) = {
      val bout = new ByteArrayOutputStream
      val gout = if (gzip) new GZIPOutputStream(bout)
      else new DataOutputStream(bout)
      val tmp = new Array[Byte](4096)
      var r = 0
      r = in.read(tmp)
      while ( {
        r >= 0
      }) {
        gout.write(tmp, 0, r)
        r = in.read(tmp)
      }
      gout.flush()
      gout.close()
      in.close()
      bout.toByteArray
    }
  }

}
