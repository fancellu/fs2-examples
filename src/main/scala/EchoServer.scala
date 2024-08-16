import cats.effect.*
import fs2.*
import cats.effect.std.Console
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.io.net.Network

object EchoServer extends IOApp.Simple:

  private case object ExitException extends Exception("Exit")
  private case object KillServerException extends Exception("Kill Server")

  // Takes a string and writes it to the client socket
  // We use this custom pipe a few times

  private def encodeAndWrite[F[_]](
      clientSocket: io.net.Socket[F]
  ): Pipe[F, String, Unit] =
    _.through(text.utf8.encode).through(clientSocket.writes)

  private def handleClient[F[_]: Concurrent: Network: Console](
      clientSocket: io.net.Socket[F]
  ) =
    val onNewConnection = Stream // happens on each new client connection
      .emit("Hello from the server!\n\r")
      .through(encodeAndWrite(clientSocket))
      ++ Stream.eval(
        Console[F].println(
          s"New client connection"
        )
      )

    onNewConnection ++
      clientSocket.reads
        .through(text.utf8.decode)
        .through(text.lines)
        .flatMap { line =>
          if line == "EXIT" then Stream.raiseError[F](ExitException)
          else if line == "KILLSERVER" then
            Stream.raiseError[F](KillServerException)
          else Stream.emit(line + "\n\r")
        }
        .through(encodeAndWrite(clientSocket))
        .handleErrorWith {
          case KillServerException => Stream.raiseError(KillServerException)
          case ExitException =>
            Stream.eval(
              Console[F].println("Ending connection")
            ) ++ Stream
              .emit("Byebye!\n\r")
              .through(encodeAndWrite(clientSocket))
              ++ Stream.empty // close the client socket
        }

  private def echoServer[F[_]: Concurrent: Network: Console] =
    Network[F]
      .server(port = Some(port"5555"))
      .map(handleClient[F])
      .parJoin(2) // max number of concurrent connections
      .handleErrorWith { case KillServerException =>
        Stream.eval(
          Console[F].println("Server terminated by user")
        ) ++ Stream.empty // closes the server
      }

  override def run: IO[Unit] =
    for
      _ <- IO.println("Hello, this is a simple echo server")
      server <- echoServer[IO].compile.drain
      _ <- IO.println("Server exiting")
    yield ()
