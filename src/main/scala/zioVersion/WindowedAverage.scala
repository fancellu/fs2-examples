package zioVersion

import zio.*
import zio.stream.*
import zio.nio.file.{Files, Path}

import scala.util.Try

object WindowedAverage extends ZIOAppDefault:

  private object Fahrenheit:
    def unapply(line: String): Option[Double] =
      if line.trim.nonEmpty && !line.startsWith("//") then
        Try(line.toDouble).toOption
      else None

  private val averager =
    val WINDOW = 5

    def handleError(error: Throwable): UStream[String] =
      ZStream.succeed(s"\nError: ${error.getMessage}")

    val inout = Files
      .lines(Path("testdata/fahrenheit.txt"))
      .collect { case Fahrenheit(double) =>
        double
      }
      .catchAll(error => handleError(error) *> ZStream.fail(error))
      .scan(Vector.empty[Double]) { case (list, dub) =>
        (if list.size >= WINDOW then list.drop(1) else list) :+ dub
      }
      .filter(_.length >= WINDOW)
      .map(li => li.sum / li.length)
      .map(_.toString)

    ZStream.succeed("// running average") ++ inout

  def run =
    averager
      .run(ZSink.foreach { line =>
        if line.startsWith("Error:") then Console.printLineError(line)
        else Console.printLine(line)
      })
      .catchAll(error => Console.printLineError(s"Error: ${error.getMessage}"))
