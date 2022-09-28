package littlejian.unifier

import littlejian._

import scala.language.implicitConversions

implicit def U$List[T](implicit unifier: Unifier[T]): Unifier[List[T]] = {
  implicit object U extends Unifier[List[T]] {
    override def concreteUnify(self: List[T], other: List[T]): Unifying[Unit] = (self, other) match {
      case (x :: xs, y :: ys) => for {
        _ <- x.unify(y)
        _ <- xs.unify(ys)
      } yield ()
      case (Nil, Nil) => Unifying.success(())
      case _ => Unifying.failure
    }
  }
  U
}
implicit def U$Seq[T](implicit unify: Unifier[T]): Unifier[Seq[T]] = {
  implicit object U extends Unifier[Seq[T]] {
    override def concreteUnify(self: Seq[T], other: Seq[T]): Unifying[Unit] =
      if(self.isEmpty && other.isEmpty) Unifying.success(())
      else if(self.nonEmpty && other.nonEmpty) for {
        _ <- self.head.unify(other.head)
        _ <- self.tail.unify(other.tail)
      } yield ()
      else Unifying.failure
  }
  U
}