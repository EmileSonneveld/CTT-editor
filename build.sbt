import org.scalajs.core.tools.linker.ModuleInitializer

name := "CTT-editor"

version := "0.1"

scalaVersion := "2.12.7"


enablePlugins(ScalaJSPlugin)

name := "CTT-editor"
scalaVersion := "2.12.6" // or any other Scala version >= 2.10.2

// This is an application with a main method
scalaJSUseMainModuleInitializer := true
mainClass in Compile := Some("main.scala.CttEditor")


libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.6"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.144-R12"

// https://mvnrepository.com/artifact/com.typesafe.play/play-json
libraryDependencies += "com.typesafe.play" %%% "play-json" % "2.7.0-RC2"


scalacOptions ++= Seq("-feature")
