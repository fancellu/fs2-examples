import cats.effect.{IO, IOApp}
import fs2.{Stream, text}
import fs2.io.file.{Files, Path}
import fs2.io.{stdout, stderr}

import scala.util.Try

object Converter1 extends IOApp.Simple:

  private object Fahrenheit:
    // Using extractor to handle empty lines, comment lines and non valid strings
    def unapply(line: String): Option[Double] =
      if line.trim.nonEmpty && !line.startsWith("//") then
        Try(line.toDouble).toOption
      else None

  private val converter =
    def f2c(f: Double): Double = (f - 32.0) * (5.0 / 9.0)

    // Stream is terminated after the first error, hence the need to emit to stderr
    def handleError(error: Throwable): Stream[IO, Unit] =
      Stream
        .emit(s"\nError: ${error.getMessage}")
        .through(text.utf8.encode)
        .through(stderr[IO]())

    val inout: Stream[IO, String] = Files[IO]
      .readUtf8Lines(Path("testdata/fahrenheit.txt"))
      .handleErrorWith(handleError) // Handle potential errors here
      .collect { case Fahrenheit(double) =>
        f"${f2c(double)}%.2f"
      }

    // I prepend a header line before the converted values
    (Stream.emit("// celsius values") ++ inout)
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(stdout[IO]())

  def run: IO[Unit] =
    converter.compile.drain
