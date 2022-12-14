package littlejian

import scala.annotation.targetName
import scala.language.implicitConversions
import scala.reflect.ClassTag
import littlejian.utils.*
import scala.collection.immutable.HashSet

// for GoalAbsent usages

final case class VarWithInspect[T](self: Var[T])(implicit inspect: Inspect[T]) {
  def runInspect(walker: Any => Any, x: Any): InspectResult = inspect.runInspect(walker, self, x)
}

// None: contains
// Some(Vector()): not contains
// Some(Vector(...)): uncertain
type InspectResult = Option[Vector /*orIn*/ [VarWithInspect[_]]]

// None: contains
// Some(Vector()): not contains
// Some(Vector(...)): uncertain
type InspectResults = Option[Vector /*andIn*/ [Vector /*orIn*/ [VarWithInspect[_]]]]
type AbsentStore = Vector /*and*/ [(Any /*x*/ , Vector /*orIn*/ [VarWithInspect[_]])]

object AbsentStore {
  inline def insert(self: AbsentStore, x: Any, ors: Vector /*orIn*/ [VarWithInspect[_]]): AbsentStore =
    if (ors.isEmpty) self else (x, ors) +: self

  inline def run0(walker: Any => Any, x: Any, ors: Vector /*orIn*/ [VarWithInspect[_]]): InspectResult = {
    if (ors.isEmpty) throw new IllegalArgumentException()
    ors.map(_.runInspect(walker, x)).reduce(_.orIn(_))
  }

  inline def run(walker: Any => Any, store: AbsentStore): Option[AbsentStore] = traverse(store.map {
    case (x, clauses) => run0(walker, x, clauses) map { result => (x, result) }
  }).map(_.filter(_._2.nonEmpty))

  inline def print0(x: Any, ors: Vector /*orIn*/ [VarWithInspect[_]]): String = ors.map(wi => s"${x}.absent(${wi.self})").mkString(" || ")

  inline def print(self: AbsentStore): String = if (self.isEmpty) "" else self.map({ case (x, clauses) => print0(x, clauses) }).mkString("\n")
}

implicit class InspectResultOps(self: InspectResult) {
  inline def orIn(other: InspectResult): InspectResult = for {
    xs <- self
    ys <- other
  } yield xs ++ ys
}

object InspectResult {
  inline def apply(x: Boolean): InspectResult = if (x) Contains else NotContains

  val Contains: InspectResult = None
  val NotContains: InspectResult = Some(Vector.empty)

  inline def Maybe[T](x: Var[T])(implicit inspect: Inspect[T]): InspectResult = Some(Vector(VarWithInspect(x)(inspect)))
}

trait Inspector {
  def apply[T](x: VarOr[T])(implicit inspect: Inspect[T]): InspectResult
}

trait Inspect[T] {
  def inspect(rec: Inspector, self: T, x: Any): InspectResult
}

private val inspectRecHistory = new Parameter[HashSet[Any]]

implicit class InspectOps[T](self: Inspect[T]) {
  def runInspect(walker: Any => Any, v: VarOr[T], x: Any): InspectResult = {
    if (v == x) return InspectResult.Contains
    val value: VarOr[T] = walker(v).asInstanceOf
    if (value == x) return InspectResult.Contains
    val history = inspectRecHistory.get.getOrElse(HashSet.empty)
    if (history.contains(value)) return InspectResult.NotContains
    if (value.isInstanceOf[Var[_]]) return InspectResult.Maybe(value.asInstanceOf[Var[T]])(self)
    inspectRecHistory.callWith(history.incl(value)) {
      self.inspect(new Inspector() {
        override def apply[T](arg: VarOr[T])(implicit inspect: Inspect[T]): InspectResult = inspect.runInspect(walker, arg, x)
      }, value.asInstanceOf, x)
    }
  }
}

object Inspect {

  import shapeless3.deriving.*

  given inspectSum[A] (using inst: K0.CoproductInstances[Inspect, A]): Inspect[A] with
    def inspect(rec: Inspector, self: A, x: Any): InspectResult = inst.fold(self)(
      [t] => (i: Inspect[t], t0: t) => i.inspect(rec, t0, x)
    )

  given inspectProduct[A] (using inst: K0.ProductInstances[Inspect, A]): Inspect[A] with
    def inspect(rec: Inspector, self: A, x: Any): InspectResult = inst.foldLeft(self)(InspectResult.NotContains)(
      [t] => (acc: InspectResult, i: Inspect[t], t0: t) =>
        rec(t0)(i) orIn acc
    )

  inline def derived[A](using gen: K0.Generic[A]): Inspect[A] =
    gen.derive(inspectProduct, inspectSum)
}

implicit def I$VarOr[T](implicit x: Inspect[T]): Inspect[VarOr[T]] = x.asInstanceOf

trait AtomInspect[T] extends Inspect[T] {
  override def inspect(rec: Inspector, self: T, x: Any): InspectResult = InspectResult.NotContains
}

implicit object I$Var extends AtomInspect[Var[_]]

implicit object I$String extends AtomInspect[String]

implicit object I$BigDecimal extends AtomInspect[BigDecimal]

implicit object I$Boolean extends AtomInspect[Boolean]

implicit object I$Short extends AtomInspect[Short]

implicit object I$Int extends AtomInspect[Int]

implicit object I$Long extends AtomInspect[Long]

implicit object I$Float extends AtomInspect[Float]

implicit object I$Double extends AtomInspect[Double]

implicit object I$Integer extends AtomInspect[Integer]

implicit object I$Unit extends AtomInspect[Unit]

implicit object I$BoxedUnit extends AtomInspect[scala.runtime.BoxedUnit]

implicit def I$Seq[T](implicit i: Inspect[T]): Inspect[Seq[T]] = new Inspect[Seq[T]] {
  override def inspect(rec: Inspector, self: Seq[T], x: Any): InspectResult =
    if(self.isEmpty) InspectResult.NotContains
    else i.inspect(rec, self.head, x) orIn this.inspect(rec, self.tail, x)
}

implicit def I$Vector[T](implicit i: Inspect[T]): Inspect[Vector[T]] = I$Seq.asInstanceOf

implicit def I$List[T](implicit i: Inspect[T]): Inspect[List[T]] = I$Seq.asInstanceOf

@targetName("I$Union_") def I$Union[T, U](implicit tr: => Inspect[T], ur: => Inspect[U], tev: ClassTag[T], uev: ClassTag[U]): Inspect[T | U] = I$Union(tr, ur)(tev, uev)
def I$Union[T, U](tr: => Inspect[T], ur: => Inspect[U])(implicit tev: ClassTag[T], uev: ClassTag[U]): Inspect[T | U] = {
  lazy val t = tr
  lazy val u = ur
  val tc = tev.toClass
  val uc = uev.toClass
  if (Set(tc, uc).size != 2) throw new IllegalArgumentException("duplication")
  (rec, self, x) => {
    if (tc.isInstance(self)) t.inspect(rec, self.asInstanceOf, x)
    else u.inspect(rec, self.asInstanceOf, x)
  }
}

@targetName("I$Union_") def I$Union[A, B, C](implicit ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C]): Inspect[A | B | C] = I$Union(ar, br, cr)(aev, bev, cev)
def I$Union[A, B, C](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C])(implicit aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C]): Inspect[A | B | C] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  val ac = aev.toClass
  val bc = bev.toClass
  val cc = cev.toClass
  if (Set(ac, bc, cc).size != 3) throw new IllegalArgumentException("duplication")
  (rec, self, x) => {
    if (ac.isInstance(self)) a.inspect(rec, self.asInstanceOf, x)
    else if (bc.isInstance(self)) b.inspect(rec, self.asInstanceOf, x)
    else c.inspect(rec, self.asInstanceOf, x)
  }
}

@targetName("I$Union_") def I$Union[A, B, C, D](implicit ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D]): Inspect[A | B | C | D] = I$Union(ar, br, cr, dr)(aev, bev, cev, dev)
def I$Union[A, B, C, D](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D])(implicit aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D]): Inspect[A | B | C | D] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  val ac = aev.toClass
  val bc = bev.toClass
  val cc = cev.toClass
  val dc = dev.toClass
  if (Set(ac, bc, cc, dc).size != 4) throw new IllegalArgumentException("duplication")
  (rec, self, x) => {
    if (ac.isInstance(self)) a.inspect(rec, self.asInstanceOf, x)
    else if (bc.isInstance(self)) b.inspect(rec, self.asInstanceOf, x)
    else if (cc.isInstance(self)) c.inspect(rec, self.asInstanceOf, x)
    else d.inspect(rec, self.asInstanceOf, x)
  }
}

@targetName("I$Union_") def I$Union[A, B, C, D, E](implicit ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E]): Inspect[A | B | C | D | E] = I$Union(ar, br, cr, dr, er)(aev, bev, cev, dev, eev)
def I$Union[A, B, C, D, E](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E])(implicit aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E]): Inspect[A | B | C | D | E] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  lazy val e = er
  val ac = aev.toClass
  val bc = bev.toClass
  val cc = cev.toClass
  val dc = dev.toClass
  val ec = eev.toClass
  if (Set(ac, bc, cc, dc, ec).size != 5) throw new IllegalArgumentException("duplication")
  (rec, self, x) => {
    if (ac.isInstance(self)) a.inspect(rec, self.asInstanceOf, x)
    else if (bc.isInstance(self)) b.inspect(rec, self.asInstanceOf, x)
    else if (cc.isInstance(self)) c.inspect(rec, self.asInstanceOf, x)
    else if (dc.isInstance(self)) d.inspect(rec, self.asInstanceOf, x)
    else e.inspect(rec, self.asInstanceOf, x)
  }
}
@targetName("I$Union_") def I$Union[A, B, C, D, E, F](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F], aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E], fev: ClassTag[F]): Inspect[A | B | C | D | E | F] = I$Union(ar, br, cr, dr, er, fr)(aev, bev, cev, dev, eev, fev)
def I$Union[A, B, C, D, E, F](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F])(implicit aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E], fev: ClassTag[F]): Inspect[A | B | C | D | E | F] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  lazy val e = er
  lazy val f = fr
  val ac = aev.toClass
  val bc = bev.toClass
  val cc = cev.toClass
  val dc = dev.toClass
  val ec = eev.toClass
  val fc = fev.toClass
  if (Set(ac, bc, cc, dc, ec, fc).size != 6) throw new IllegalArgumentException("duplication")
  (rec, self, x) => {
    if (ac.isInstance(self)) a.inspect(rec, self.asInstanceOf, x)
    else if (bc.isInstance(self)) b.inspect(rec, self.asInstanceOf, x)
    else if (cc.isInstance(self)) c.inspect(rec, self.asInstanceOf, x)
    else if (dc.isInstance(self)) d.inspect(rec, self.asInstanceOf, x)
    else if (ec.isInstance(self)) e.inspect(rec, self.asInstanceOf, x)
    else f.inspect(rec, self.asInstanceOf, x)
  }
}
@targetName("I$Union_") def I$Union[A, B, C, D, E, F, G](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F], gr: => Inspect[G], aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E], fev: ClassTag[F], gev: ClassTag[G]): Inspect[A | B | C | D | E | F | G] = I$Union(ar, br, cr, dr, er, fr, gr)(aev, bev, cev, dev, eev, fev, gev)
def I$Union[A, B, C, D, E, F, G](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F], gr: => Inspect[G])(implicit aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E], fev: ClassTag[F], gev: ClassTag[G]): Inspect[A | B | C | D | E | F | G] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  lazy val e = er
  lazy val f = fr
  lazy val g = gr
  val ac = aev.toClass
  val bc = bev.toClass
  val cc = cev.toClass
  val dc = dev.toClass
  val ec = eev.toClass
  val fc = fev.toClass
  val gc = gev.toClass
  if (Set(ac, bc, cc, dc, ec, fc, gc).size != 7) throw new IllegalArgumentException("duplication")
  (rec, self, x) => {
    if (ac.isInstance(self)) a.inspect(rec, self.asInstanceOf, x)
    else if (bc.isInstance(self)) b.inspect(rec, self.asInstanceOf, x)
    else if (cc.isInstance(self)) c.inspect(rec, self.asInstanceOf, x)
    else if (dc.isInstance(self)) d.inspect(rec, self.asInstanceOf, x)
    else if (ec.isInstance(self)) e.inspect(rec, self.asInstanceOf, x)
    else if (fc.isInstance(self)) f.inspect(rec, self.asInstanceOf, x)
    else g.inspect(rec, self.asInstanceOf, x)
  }
}
@targetName("I$Union_") def I$Union[A, B, C, D, E, F, G, H](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F], gr: => Inspect[G], hr: => Inspect[H], aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E], fev: ClassTag[F], gev: ClassTag[G], hev: ClassTag[H]): Inspect[A | B | C | D | E | F | G | H] = I$Union(ar, br, cr, dr, er, fr, gr, hr)(aev, bev, cev, dev, eev, fev, gev, hev)
def I$Union[A, B, C, D, E, F, G, H](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F], gr: => Inspect[G], hr: => Inspect[H])(implicit aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E], fev: ClassTag[F], gev: ClassTag[G], hev: ClassTag[H]): Inspect[A | B | C | D | E | F | G | H] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  lazy val e = er
  lazy val f = fr
  lazy val g = gr
  lazy val h = hr
  val ac = aev.toClass
  val bc = bev.toClass
  val cc = cev.toClass
  val dc = dev.toClass
  val ec = eev.toClass
  val fc = fev.toClass
  val gc = gev.toClass
  val hc = hev.toClass
  if (Set(ac, bc, cc, dc, ec, fc, gc, hc).size != 8) throw new IllegalArgumentException("duplication")
  (rec, self, x) => {
    if (ac.isInstance(self)) a.inspect(rec, self.asInstanceOf, x)
    else if (bc.isInstance(self)) b.inspect(rec, self.asInstanceOf, x)
    else if (cc.isInstance(self)) c.inspect(rec, self.asInstanceOf, x)
    else if (dc.isInstance(self)) d.inspect(rec, self.asInstanceOf, x)
    else if (ec.isInstance(self)) e.inspect(rec, self.asInstanceOf, x)
    else if (fc.isInstance(self)) f.inspect(rec, self.asInstanceOf, x)
    else if (gc.isInstance(self)) g.inspect(rec, self.asInstanceOf, x)
    else h.inspect(rec, self.asInstanceOf, x)
  }
}
@targetName("I$Union_") def I$Union[A, B, C, D, E, F, G, H, I](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F], gr: => Inspect[G], hr: => Inspect[H], ir: => Inspect[I], aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E], fev: ClassTag[F], gev: ClassTag[G], hev: ClassTag[H], iev: ClassTag[I]): Inspect[A | B | C | D | E | F | G | H | I] = I$Union(ar, br, cr, dr, er, fr, gr, hr, ir)(aev, bev, cev, dev, eev, fev, gev, hev, iev)
def I$Union[A, B, C, D, E, F, G, H, I](ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F], gr: => Inspect[G], hr: => Inspect[H], ir: => Inspect[I])(implicit aev: ClassTag[A], bev: ClassTag[B], cev: ClassTag[C], dev: ClassTag[D], eev: ClassTag[E], fev: ClassTag[F], gev: ClassTag[G], hev: ClassTag[H], iev: ClassTag[I]): Inspect[A | B | C | D | E | F | G | H | I] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  lazy val e = er
  lazy val f = fr
  lazy val g = gr
  lazy val h = hr
  lazy val i = ir
  val ac = aev.toClass
  val bc = bev.toClass
  val cc = cev.toClass
  val dc = dev.toClass
  val ec = eev.toClass
  val fc = fev.toClass
  val gc = gev.toClass
  val hc = hev.toClass
  val ic = iev.toClass
  if (Set(ac, bc, cc, dc, ec, fc, gc, hc, ic).size != 9) throw new IllegalArgumentException("duplication")
  (rec, self, x) => {
    if (ac.isInstance(self)) a.inspect(rec, self.asInstanceOf, x)
    else if (bc.isInstance(self)) b.inspect(rec, self.asInstanceOf, x)
    else if (cc.isInstance(self)) c.inspect(rec, self.asInstanceOf, x)
    else if (dc.isInstance(self)) d.inspect(rec, self.asInstanceOf, x)
    else if (ec.isInstance(self)) e.inspect(rec, self.asInstanceOf, x)
    else if (fc.isInstance(self)) f.inspect(rec, self.asInstanceOf, x)
    else if (gc.isInstance(self)) g.inspect(rec, self.asInstanceOf, x)
    else if (hc.isInstance(self)) h.inspect(rec, self.asInstanceOf, x)
    else i.inspect(rec, self.asInstanceOf, x)
  }
}

implicit def I$Product[T, R <: Product1[T]](implicit tr: => Inspect[T]): Inspect[R] = {
  lazy val t = tr
  (rec, self, x) => rec(self._1)(t)
}
implicit def I$Product[T, U, R <: Product2[T, U]](implicit tr: => Inspect[T], ur: => Inspect[U]): Inspect[R] = {
  lazy val t = tr
  lazy val u = ur
  (rec, self, x) => rec(self._1)(t) orIn rec(self._2)(u)
}
implicit def I$Product[A, B, C, R <: Product3[A, B, C]](implicit ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C]): Inspect[R] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  (rec, self, x) => rec(self._1)(a) orIn rec(self._2)(b) orIn rec(self._3)(c)
}
implicit def I$Product[A, B, C, D, R <: Product4[A, B, C, D]](implicit ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D]): Inspect[R] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  (rec, self, x) => rec(self._1)(a) orIn rec(self._2)(b) orIn rec(self._3)(c) orIn rec(self._4)(d)
}
implicit def I$Product[A, B, C, D, E, R <: Product5[A, B, C, D, E]](implicit ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E]): Inspect[R] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  lazy val e = er
  (rec, self, x) => rec(self._1)(a) orIn rec(self._2)(b) orIn rec(self._3)(c) orIn rec(self._4)(d) orIn rec(self._5)(e)
}
implicit def I$Product[A, B, C, D, E, F, R <: Product6[A, B, C, D, E, F]](implicit ar: => Inspect[A], br: => Inspect[B], cr: => Inspect[C], dr: => Inspect[D], er: => Inspect[E], fr: => Inspect[F]): Inspect[R] = {
  lazy val a = ar
  lazy val b = br
  lazy val c = cr
  lazy val d = dr
  lazy val e = er
  lazy val f = fr
  (rec, self, x) => rec(self._1)(a) orIn rec(self._2)(b) orIn rec(self._3)(c) orIn rec(self._4)(d) orIn rec(self._5)(e) orIn rec(self._6)(f)
}