package littlejian.ext

import littlejian._

implicit class VarOrOptionOps[T](self: VarOr[Option[VarOr[T]]]) {
  def elim[U](onEmpty: => Rel[U])(onSome: VarOr[T] => Rel[U])(implicit o: Unify[Option[VarOr[T]]], u: Unify[U]): Rel[U] = conde(
    (self === None) >> onEmpty,
    for {
      v <- self.is[T](Some(_))
      result <- onSome(v)
    } yield result
  )
}