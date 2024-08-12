import cats.effect._
import fs2._
import scala.concurrent.duration._
import cats.effect.std._

// FS2 cats.effect.Queue example using for comprehension
// Both streams emit nothing, but are effectful, communicating via the queue and updating a sum via the ref

object QueueExample extends IOApp.Simple {

  // producer stream emits incrementing int every 100ms into queue
  private def producer(queue: Queue[IO, Int]): Stream[IO, Nothing] =
    Stream
      .iterate(0)(_ + 1)
      .covary[IO]
      .metered(100.millis)
      .evalMap(i => IO.println(s"Producing $i") *> queue.offer(i))
      .drain

  // consumer stream consumes from queue and adds int value to sum via ref
  private def consumer(queue: Queue[IO, Int], ref: Ref[IO, Int]): Stream[IO, Nothing] =
    Stream
      .fromQueueUnterminated(queue)
      .evalMap(i => IO.println(s"Consuming $i") *> ref.update(_ + i))
      .drain

  // print sum from ref
  private def printSum(ref: Ref[IO, Int]): Stream[IO, Unit] =
    Stream.eval(ref.get).flatMap(sum => Stream.eval(IO.println(s"Sum: $sum")))


  override def run: IO[Unit] = {
    val out: Stream[IO, Unit] = for {
      queue <- Stream.eval(Queue.unbounded[IO, Int])
      ref <- Stream.eval(Ref.of[IO, Int](0))
      // merge producer and consumer streams, interrupt after 1s, then print sum
      mergedStream <- producer(queue).merge(consumer(queue, ref))
        .interruptAfter(1.second) ++ printSum(ref)
    } yield mergedStream

    out.compile.drain
  }


}