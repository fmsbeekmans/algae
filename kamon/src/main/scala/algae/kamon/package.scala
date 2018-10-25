package algae

import _root_.kamon.Kamon
import algae.counting.CounterIncrement
import algae.mtl.MonadLog
import algae.syntax.counting._
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.{Applicative, Foldable, Monoid}

package object kamon {
  private[this] def dispatch[F[_], G[_], E](ge: G[E])(
    implicit
    F: Sync[F],
    G: Foldable[G],
    E: CounterIncrement[E]
  ): F[Unit] = {
    def count(e: E): F[Unit] =
      F.delay {
        Kamon
          .counter(e.counterName)
          .refine(e.tags)
          .increment(e.times)
      }

    ge.foldLeft(F.unit)(_ >> count(_))
  }

  def createCounting[F[_], G[_], E](monadLog: MonadLog[F, G[E]])(
    implicit
    F: Sync[F],
    A: Applicative[G],
    G: Foldable[G],
    E: CounterIncrement[E]
  ): Counting[F, G, E] =
    Counting.create[F, G, E](monadLog, dispatch[F, G, E])

  def createCountingNow[F[_], G[_], E](
    implicit
    F: Sync[F],
    A: Applicative[G],
    G: Foldable[G],
    M: Monoid[G[E]],
    E: CounterIncrement[E]
  ): CountingNow[F, G, E] =
    CountingNow.create[F, G, E](dispatch[F, G, E])
}
