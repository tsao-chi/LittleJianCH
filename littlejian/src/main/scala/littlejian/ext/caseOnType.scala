package littlejian.ext

import littlejian._

import scala.reflect.ClassTag

implicit class CaseOnTypeOps[T, R](x: VarOr[T]) {
  def caseOnType[U <: T](isType: (_ >: VarOr[U]) => Rel[R])(isNotType: Rel[R])(implicit t: ClassTag[U], u: Unify[U], r: Unify[R]) = conde(
    for {
      v <- x.cast[U]
      r <- isType(v)
    } yield r,
    x.isNotType[U] >> isNotType
  )

  def isOr[U <: T, A](maker: (_ >: VarOr[A]) => U)(isType: VarOr[A] => Rel[R])(isNotType: Rel[R])
                           (implicit t: ClassTag[U], u: Unify[U], r: Unify[R], uu: Unify[T]) = conde(
    for {
      a <- x.is[A](maker)
      r <- isType(a)
    } yield r,
    x.isNotType[U] >> isNotType
  )

  def isOr[U <: T, A, B](maker: (_ >: VarOr[A], _ >: VarOr[B]) => U)(isType: (VarOr[A], VarOr[B]) => Rel[R])(isNotType: Rel[R])
                              (implicit t: ClassTag[U], u: Unify[U], r: Unify[R], uu: Unify[T]) = conde(
    for {
      (a, b) <- x.is[A, B](maker)
      r <- isType(a, b)
    } yield r,
    x.isNotType[U] >> isNotType
  )

  def caseOnType[U <: T, A](maker: (_ >: VarOr[A]) => U)(isType: U => Rel[R])(isNotType: Rel[R])
                           (implicit t: ClassTag[U], u: Unify[U], r: Unify[R], uu: Unify[T]) = conde(
    for {
      a <- fresh[A]
      made = maker(a)
      _ <- x === made
      r <- isType(made)
    } yield r,
    x.isNotType[U] >> isNotType
  )
}
