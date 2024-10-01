package zioVersion

import zio.*
import zio.stream.*
import zio.nio.file.{Files, Path}

import scala.util.Try

object Converter1 extends ZIOAppDefault:

  private object Fahrenheit:
    def unapply(line: String): Option[Double] =
      if line.trim.nonEmpty && !line.startsWith("//") then
        Try(line.toDouble).toOption
      else None

  val files: List[Path] =
    List(
      Path("testdata/fahrenheit.txt"), // read in
      Path("testdata/filenotfound.txt"), // error: file not found
      Path("testdata/fahrenheit2.txt") // read in
    )

  private val converter =
    def f2c(f: Double): Double = (f - 32.0) * (5.0 / 9.0)

    def processFile(file: Path): UStream[Either[String, String]] =
      Files
        .lines(file)
        .collect { case Fahrenheit(double) =>
          Right(f"${f2c(double)}%.2f")
        }
        .catchAll(error =>
          ZStream.succeed(
            Left(s"Error: ${error.getMessage})")
          )
        )

    val inout: UStream[Either[String, String]] =
      ZStream.fromIterable(files).flatMap(processFile)

    ZStream.succeed(Right("// celsius values")) ++ inout

  def run =
    for output <- converter.run(ZSink.foreach {
        case Left(error) => Console.printLineError(error)
        case Right(line) => Console.printLine(line)
      })
    yield output
