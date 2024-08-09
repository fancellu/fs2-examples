import cats.effect.{IO, IOApp}
import fs2.{Stream, text}
import fs2.io.file.{Files, Path}
import fs2.io.{stdout, stderr}

import scala.util.Try

object WindowedAverage extends IOApp.Simple:

  private object Fahrenheit:
    // Using extractor to handle empty lines, comment lines and non valid strings
    def unapply(line: String): Option[Double] =
      if line.trim.nonEmpty && !line.startsWith("//") then
        Try(line.toDouble).toOption
      else None

  private val averager =

    // Stream is terminated after the first error, hence the need to emit to stderr
    def handleError(error: Throwable): Stream[IO, Unit] =
      Stream
        .emit(s"\nError: ${error.getMessage}")
        .through(text.utf8.encode)
        .through(stderr[IO]())

    val WINDOW = 5

    val inout = Files[IO]
      .readUtf8Lines(Path("testdata/fahrenheit.txt"))
      .handleErrorWith(handleError) // Handle potential errors here
      .collect { case Fahrenheit(double) =>
        double
      }
      .scan(Vector.empty[Double]) { case (list, dub) =>
        (if list.size >= WINDOW then list.drop(1) else list) :+ dub
      }
      .filter(_.length >= WINDOW)
      .map(li => li.sum / li.length)
      .map(_.toString)

    // I prepend a header line before the averaged values
    (Stream.emit("// running average") ++ inout)
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(stdout[IO]())

  def run: IO[Unit] =
    averager.compile.drain
