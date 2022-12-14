package littlejian.data

import littlejian._
import littlejian.ext._

sealed trait Mapping[K, V] derives Unify, Inspect, DeepWalk {
  def get(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Rel[V]

  def getOption(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Rel[Option[VarOr[V]]]

  def notContains(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Goal

  def updated(key: VarOr[K], value: VarOr[V]): Mapping[K, V] = MappingNonEmpty(key, value, this)
}

implicit class VarOrMappingOps[K, V](self: VarOr[Mapping[K, V]]) {
  private def force(implicit uK: Unify[K], uV: Unify[V]): GoalWith[Mapping[K, V]] = GoalWith(k => conde(
    (self === MappingEmpty()) && k(MappingEmpty()),
    for {
      x <- self.as(MappingNonEmpty[K, V](_, _, _))
      _ <- k(x)
    } yield ()
  ))

  def get(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Rel[V] = force.flatMap(_.get(k))

  def getOption(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Rel[Option[VarOr[V]]] = force.flatMap(_.getOption(k))

  def notContains(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Goal = force.flatMap(_.notContains(k)).goal

  def updated(key: VarOr[K], value: VarOr[V]): Mapping[K, V] = MappingNonEmpty(key, value, self)

  def toLList(implicit uK: Unify[K], uV: Unify[V]): Rel[LList[Tup2[K, V]]] = conde(
    (self === Mapping.empty) >> LList.empty,
    for {
      (key, value, tail) <- self.is[K, V, Mapping[K, V]](MappingNonEmpty(_, _, _))
      rest <- tail.toLList
    } yield (key, value) :: rest
  )
}

case class MappingEmpty[K, V]() extends Mapping[K, V] derives Unify, Inspect, DeepWalk {
  override def get(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Rel[V] = Rel.failure

  override def getOption(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Rel[Option[VarOr[V]]] = None

  override def notContains(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Goal = Goal.success
}

case class MappingNonEmpty[K, V](key: VarOr[K], value: VarOr[V], next: VarOr[Mapping[K, V]]) extends Mapping[K, V] derives Unify, Inspect, DeepWalk {
  override def get(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Rel[V] = compare(key, k) {
    value
  } {
    next.get(k)
  }

  override def getOption(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Rel[Option[VarOr[V]]] = compare(key, k) {
    Some(value)
  } {
    next.getOption(k)
  }

  override def notContains(k: VarOr[K])(implicit uK: Unify[K], uV: Unify[V]): Goal = compare(key, k) {
    Goal.failure
  } {
    next.notContains(k)(uK, uV)
  }
}

object Mapping {
  def empty[K, V]: Mapping[K, V] = MappingEmpty()

  def from[K, V](xs: Seq[(K, V)]): Mapping[K, V] = if (xs.isEmpty) MappingEmpty() else MappingNonEmpty(xs.head._1, xs.head._2, from(xs.tail))

  def apply[K, V](xs: (K, V)*): Mapping[K, V] = from(xs)

  def from[K, V](xs: VarOr[LList[Tup2[K, V]]])(implicit uK: Unify[K], uV: Unify[V]): Rel[Mapping[K, V]] = xs.elim(Mapping.empty)((x, xs) => for {
    tail <- from(xs)
    (key, value) <- x.is[K, V]((_, _))
  } yield tail.updated(key, value))
}