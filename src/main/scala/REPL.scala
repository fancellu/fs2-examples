import cats.effect.*
import cats.syntax.all.*

object REPL extends IOApp.Simple:

  private val repl: IO[Unit] =
    for
      either <- IO.println(">>> ") *> IO.readLine.attempt
      _ <- either match
        case Right(input) =>
          IO.println(s"You entered: $input") *>
            (if input == "KILLSERVER" then IO.unit else repl)
        // We kill the server on EOF (from IO.readLine), i.e. if they press ctrl-d/ctrl-z, or any other error
        case Left(_) => IO.unit
    yield ()

  override def run: IO[Unit] =
    for
      _ <- IO.println("Hello, this is a simple REPL") *> repl
      _ <- IO.println("REPL exiting")
    yield ()
