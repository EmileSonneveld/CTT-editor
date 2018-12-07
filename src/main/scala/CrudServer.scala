import java.net.InetSocketAddress
import java.io._
import java.util
import java.util.zip.GZIPOutputStream

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

object CrudServer extends App {
  var server = HttpServer.create(new InetSocketAddress(8000), 0)
  server.createContext("/", new StaticHandler())
  server.setExecutor(null) // creates a default executor
  server.start()


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
      var res: Asset = null
      var status: Int = 500 // If not changed, we throw an error
      try {
        if (path.contains("..")) throw new Exception("Supspicious path")
        path = path.substring(1)
        path = path.replaceAll("//", "/")
        if (new File(pathToRoot + path + "index.html").exists) path += "index.html"
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
        status = 200
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

    @throws[IOException]
    private def processFile(path: String, f: File, gzip: Boolean): Unit = {
      if (!f.isDirectory) data.put(path + f.getName, new Asset(readResource(new FileInputStream(f), gzip)))
      if (f.isDirectory) {
        import scala.collection.JavaConversions._
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
