package zioVersion

import zio.*
import zio.stream.*

object QueueExample extends ZIOAppDefault:

  // producer stream emits incrementing int every 100ms into queue
  private def producer(queue: Queue[Int]): UStream[Nothing] =
    ZStream
      .iterate(0)(_ + 1)
      .schedule(Schedule.spaced(100.milliseconds))
      .mapZIO(i => Console.printLine(s"Producing $i").ignore *> queue.offer(i))
      .drain

  // consumer stream consumes from queue and adds int value to sum via ref
  private def consumer(
      queue: Queue[Int],
      ref: Ref[Int]
  ): UStream[Nothing] =
    ZStream
      .fromQueue(queue)
      .mapZIO(i =>
        Console.printLine(s"Consuming $i").ignore *> ref.update(_ + i)
      )
      .drain

  // print sum from ref
  private def printSum(ref: Ref[Int]): UIO[Unit] =
    ref.get.flatMap(sum => Console.printLine(s"Sum: $sum").ignore)

  override def run: UIO[Unit] =
    for
      queue <- Queue.unbounded[Int]
      ref <- Ref.make(0)
      _ <- (producer(queue) merge consumer(queue, ref))
        .interruptAfter(1.second)
        .runDrain
      _ <- printSum(ref)
    yield ()
