package zioVersion

import zio.*
import zio.Console.*

object REPL extends ZIOAppDefault:

  private val repl: Task[Unit] =
    for
      _ <- printLine(">>> ")
      input <- readLine
      _ <- printLine(s"You entered: $input")
      _ <- repl.unless(input == "KILLSERVER")
    yield ()

  def run: Task[Unit] =
    for
      _ <- printLine("Hello, this is a simple REPL")
      _ <- repl
      _ <- printLine("REPL exiting")
    yield ()
