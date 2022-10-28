package littlejian

type Num = Byte | Short | Int | Long | Float | Double

enum NumTag:
  case Byte
  case Short
  case Int
  case Long
  case Float
  case Double

implicit val U$Num: Unify[Num] = U$Union[Byte, Short, Int, Long, Float, Double]

enum NumOp2:
  case Add
  case Sub
  case Mul

sealed trait GoalNumOp2 extends GoalBasic {
  def rel: NumOp2

  def tag: NumTag

  def x: Num | Var[_ <: Num]

  def y: Num | Var[_ <: Num]

  def result: Num | Var[_ <: Num]

  override def execute(state: State): IterableOnce[State] = state.num.insert(state, this)

  def walk(subst: Subst): GoalNumOp2

  override def toString: String = {
    val relName = rel match {
      case NumOp2.Add => "+"
      case NumOp2.Sub => "-"
      case NumOp2.Mul => "*"
    }
    s"$x $relName $y === $result"
  }
}

object GoalNumOp2 {
  def unapply(self: GoalNumOp2): Some[(NumOp2, NumTag, Num | Var[_ <: Num], Num | Var[_ <: Num], Num | Var[_ <: Num])] = Some((self.rel, self.tag, self.x, self.y, self.result))
}

final case class GoalNumOp2Byte(rel: NumOp2, x: VarOr[Byte], y: VarOr[Byte], result: VarOr[Byte]) extends GoalNumOp2 {
  override def tag = NumTag.Byte

  override def walk(subst: Subst): GoalNumOp2Byte = GoalNumOp2Byte(rel, subst.walk(x), subst.walk(y), subst.walk(result))
}

final case class GoalNumOp2Short(rel: NumOp2, x: VarOr[Short], y: VarOr[Short], result: VarOr[Short]) extends GoalNumOp2 {
  override def tag = NumTag.Short

  override def walk(subst: Subst): GoalNumOp2Short = GoalNumOp2Short(rel, subst.walk(x), subst.walk(y), subst.walk(result))
}

final case class GoalNumOp2Int(rel: NumOp2, x: VarOr[Int], y: VarOr[Int], result: VarOr[Int]) extends GoalNumOp2 {
  override def tag = NumTag.Int

  override def walk(subst: Subst): GoalNumOp2Int = GoalNumOp2Int(rel, subst.walk(x), subst.walk(y), subst.walk(result))
}

final case class GoalNumOp2Long(rel: NumOp2, x: VarOr[Long], y: VarOr[Long], result: VarOr[Long]) extends GoalNumOp2 {
  override def tag = NumTag.Long

  override def walk(subst: Subst): GoalNumOp2Long = GoalNumOp2Long(rel, subst.walk(x), subst.walk(y), subst.walk(result))
}

final case class GoalNumOp2Float(rel: NumOp2, x: VarOr[Float], y: VarOr[Float], result: VarOr[Float]) extends GoalNumOp2 {
  override def tag = NumTag.Float

  override def walk(subst: Subst): GoalNumOp2Float = GoalNumOp2Float(rel, subst.walk(x), subst.walk(y), subst.walk(result))
}

final case class GoalNumOp2Double(rel: NumOp2, x: VarOr[Double], y: VarOr[Double], result: VarOr[Double]) extends GoalNumOp2 {
  override def tag = NumTag.Double

  override def walk(subst: Subst): GoalNumOp2Double = GoalNumOp2Double(rel, subst.walk(x), subst.walk(y), subst.walk(result))
}

final case class Boundary[T](x: T, eq: Boolean)

implicit class BoundaryVarOrOps[T](self: Boundary[VarOr[T]]) {
  def walk(subst: Subst): Boundary[VarOr[T]] = Boundary(subst.walk(self.x), self.eq)
}

sealed trait GoalNumRange extends GoalBasic {
  def tag: NumTag

  def low: Option[Boundary[_ <: Num | Var[_ <: Num]]]

  def high: Option[Boundary[_ <: Num | Var[_ <: Num]]]

  def num: Num | Var[_ <: Num]

  override def execute(state: State): IterableOnce[State] = ???

  def walk(subst: Subst): GoalNumRange

  if (low.isEmpty && high.isEmpty) {
    throw new IllegalArgumentException("At least one boundary must be defined")
  }
}

final case class GoalNumRangeByte(num: VarOr[Byte], low: Option[Boundary[VarOr[Byte]]], high: Option[Boundary[VarOr[Byte]]]) extends GoalNumRange {
  override def tag = NumTag.Byte

  override def walk(subst: Subst): GoalNumRangeByte = GoalNumRangeByte(num = subst.walk(num), low = low.map(_.walk(subst)), high = high.map(_.walk(subst)))
}

final case class GoalNumRangeShort(num: VarOr[Short], low: Option[Boundary[VarOr[Short]]], high: Option[Boundary[VarOr[Short]]]) extends GoalNumRange {
  override def tag = NumTag.Short

  override def walk(subst: Subst): GoalNumRangeShort = GoalNumRangeShort(num = subst.walk(num), low = low.map(_.walk(subst)), high = high.map(_.walk(subst)))
}

final case class GoalNumRangeInt(num: VarOr[Int], low: Option[Boundary[VarOr[Int]]], high: Option[Boundary[VarOr[Int]]]) extends GoalNumRange {
  override def tag = NumTag.Int

  override def walk(subst: Subst): GoalNumRangeInt = GoalNumRangeInt(num = subst.walk(num), low = low.map(_.walk(subst)), high = high.map(_.walk(subst)))
}

final case class GoalNumRangeLong(num: VarOr[Long], low: Option[Boundary[VarOr[Long]]], high: Option[Boundary[VarOr[Long]]]) extends GoalNumRange {
  override def tag = NumTag.Long

  override def walk(subst: Subst): GoalNumRangeLong = GoalNumRangeLong(num = subst.walk(num), low = low.map(_.walk(subst)), high = high.map(_.walk(subst)))
}

final case class GoalNumRangeFloat(num: VarOr[Float], low: Option[Boundary[VarOr[Float]]], high: Option[Boundary[VarOr[Float]]]) extends GoalNumRange {
  override def tag = NumTag.Float

  override def walk(subst: Subst): GoalNumRangeFloat = GoalNumRangeFloat(num = subst.walk(num), low = low.map(_.walk(subst)), high = high.map(_.walk(subst)))
}

final case class GoalNumRangeDouble(num: VarOr[Double], low: Option[Boundary[VarOr[Double]]], high: Option[Boundary[VarOr[Double]]]) extends GoalNumRange {
  override def tag = NumTag.Double

  override def walk(subst: Subst): GoalNumRangeDouble = GoalNumRangeDouble(num = subst.walk(num), low = low.map(_.walk(subst)), high = high.map(_.walk(subst)))
}

implicit class GoalNumOpOps(self: GoalNumOp2) {
  def is2: Boolean = {
    val a = if (self.x.isInstanceOf[Num]) 1 else 0
    val b = if (self.y.isInstanceOf[Num]) 1 else 0
    val c = if (self.result.isInstanceOf[Num]) 1 else 0
    a + b + c >= 2
  }

  def solve2: Unifying[Unit] = self match {
    case GoalNumOp2Byte(NumOp2.Add, x: Byte, y: Byte, rel) => rel.unify((x + y).asInstanceOf[Byte])
    case GoalNumOp2Byte(NumOp2.Add, x, y: Byte, rel: Byte) => x.unify((rel - y).asInstanceOf[Byte])
    case GoalNumOp2Byte(NumOp2.Add, x: Byte, y, rel: Byte) => y.unify((rel - x).asInstanceOf[Byte])
    case GoalNumOp2Byte(NumOp2.Sub, x: Byte, y: Byte, rel) => rel.unify((x - y).asInstanceOf[Byte])
    case GoalNumOp2Byte(NumOp2.Sub, x, y: Byte, rel: Byte) => x.unify((rel + y).asInstanceOf[Byte])
    case GoalNumOp2Byte(NumOp2.Sub, x: Byte, y, rel: Byte) => y.unify((x - rel).asInstanceOf[Byte])
    case GoalNumOp2Byte(NumOp2.Mul, x: Byte, y: Byte, rel) => rel.unify((x * y).asInstanceOf[Byte])
    case GoalNumOp2Byte(NumOp2.Mul, x, y: Byte, rel: Byte) => if (rel % y == 0) x.unify((rel / y).asInstanceOf[Byte]) else Unifying.failure
    case GoalNumOp2Byte(NumOp2.Mul, x: Byte, y, rel: Byte) => if (rel % x == 0) y.unify((rel / x).asInstanceOf[Byte]) else Unifying.failure
    case GoalNumOp2Byte(_, _, _, _) => throw new IllegalArgumentException("not a 2-arg goal")
    case GoalNumOp2Short(NumOp2.Add, x: Short, y: Short, rel) => rel.unify((x + y).asInstanceOf[Short])
    case GoalNumOp2Short(NumOp2.Add, x, y: Short, rel: Short) => x.unify((rel - y).asInstanceOf[Short])
    case GoalNumOp2Short(NumOp2.Add, x: Short, y, rel: Short) => y.unify((rel - x).asInstanceOf[Short])
    case GoalNumOp2Short(NumOp2.Sub, x: Short, y: Short, rel) => rel.unify((x - y).asInstanceOf[Short])
    case GoalNumOp2Short(NumOp2.Sub, x, y: Short, rel: Short) => x.unify((rel + y).asInstanceOf[Short])
    case GoalNumOp2Short(NumOp2.Sub, x: Short, y, rel: Short) => y.unify((x - rel).asInstanceOf[Short])
    case GoalNumOp2Short(NumOp2.Mul, x: Short, y: Short, rel) => rel.unify((x * y).asInstanceOf[Short])
    case GoalNumOp2Short(NumOp2.Mul, x, y: Short, rel: Short) => if (rel % y == 0) x.unify((rel / y).asInstanceOf[Short]) else Unifying.failure
    case GoalNumOp2Short(NumOp2.Mul, x: Short, y, rel: Short) => if (rel % x == 0) y.unify((rel / x).asInstanceOf[Short]) else Unifying.failure
    case GoalNumOp2Short(_, _, _, _) => throw new IllegalArgumentException("not a 2-arg goal")
    case GoalNumOp2Int(NumOp2.Add, x: Int, y: Int, rel) => rel.unify(x + y)
    case GoalNumOp2Int(NumOp2.Add, x, y: Int, rel: Int) => x.unify(rel - y)
    case GoalNumOp2Int(NumOp2.Add, x: Int, y, rel: Int) => y.unify(rel - x)
    case GoalNumOp2Int(NumOp2.Sub, x: Int, y: Int, rel) => rel.unify(x - y)
    case GoalNumOp2Int(NumOp2.Sub, x, y: Int, rel: Int) => x.unify(rel + y)
    case GoalNumOp2Int(NumOp2.Sub, x: Int, y, rel: Int) => y.unify(x - rel)
    case GoalNumOp2Int(NumOp2.Mul, x: Int, y: Int, rel) => rel.unify(x * y)
    case GoalNumOp2Int(NumOp2.Mul, x, y: Int, rel: Int) => if (rel % y == 0) x.unify(rel / y) else Unifying.failure
    case GoalNumOp2Int(NumOp2.Mul, x: Int, y, rel: Int) => if (rel % x == 0) y.unify(rel / x) else Unifying.failure
    case GoalNumOp2Int(_, _, _, _) => throw new IllegalArgumentException("not a 2-arg goal")
    case GoalNumOp2Long(NumOp2.Add, x: Long, y: Long, rel) => rel.unify(x + y)
    case GoalNumOp2Long(NumOp2.Add, x, y: Long, rel: Long) => x.unify(rel - y)
    case GoalNumOp2Long(NumOp2.Add, x: Long, y, rel: Long) => y.unify(rel - x)
    case GoalNumOp2Long(NumOp2.Sub, x: Long, y: Long, rel) => rel.unify(x - y)
    case GoalNumOp2Long(NumOp2.Sub, x, y: Long, rel: Long) => x.unify(rel + y)
    case GoalNumOp2Long(NumOp2.Sub, x: Long, y, rel: Long) => y.unify(x - rel)
    case GoalNumOp2Long(NumOp2.Mul, x: Long, y: Long, rel) => rel.unify(x * y)
    case GoalNumOp2Long(NumOp2.Mul, x, y: Long, rel: Long) => if (rel % y == 0) x.unify(rel / y) else Unifying.failure
    case GoalNumOp2Long(NumOp2.Mul, x: Long, y, rel: Long) => if (rel % x == 0) y.unify(rel / x) else Unifying.failure
    case GoalNumOp2Long(_, _, _, _) => throw new IllegalArgumentException("not a 2-arg goal")
    case GoalNumOp2Float(NumOp2.Add, x: Float, y: Float, rel) => rel.unify(x + y)
    case GoalNumOp2Float(NumOp2.Add, x, y: Float, rel: Float) => x.unify(rel - y)
    case GoalNumOp2Float(NumOp2.Add, x: Float, y, rel: Float) => y.unify(rel - x)
    case GoalNumOp2Float(NumOp2.Sub, x: Float, y: Float, rel) => rel.unify(x - y)
    case GoalNumOp2Float(NumOp2.Sub, x, y: Float, rel: Float) => x.unify(rel + y)
    case GoalNumOp2Float(NumOp2.Sub, x: Float, y, rel: Float) => y.unify(x - rel)
    case GoalNumOp2Float(NumOp2.Mul, x: Float, y: Float, rel) => rel.unify(x * y)
    case GoalNumOp2Float(NumOp2.Mul, x, y: Float, rel: Float) => x.unify(rel / y)
    case GoalNumOp2Float(NumOp2.Mul, x: Float, y, rel: Float) => y.unify(rel / x)
    case GoalNumOp2Float(_, _, _, _) => throw new IllegalArgumentException("not a 2-arg goal")
    case GoalNumOp2Double(NumOp2.Add, x: Double, y: Double, rel) => rel.unify(x + y)
    case GoalNumOp2Double(NumOp2.Add, x, y: Double, rel: Double) => x.unify(rel - y)
    case GoalNumOp2Double(NumOp2.Add, x: Double, y, rel: Double) => y.unify(rel - x)
    case GoalNumOp2Double(NumOp2.Sub, x: Double, y: Double, rel) => rel.unify(x - y)
    case GoalNumOp2Double(NumOp2.Sub, x, y: Double, rel: Double) => x.unify(rel + y)
    case GoalNumOp2Double(NumOp2.Sub, x: Double, y, rel: Double) => y.unify(x - rel)
    case GoalNumOp2Double(NumOp2.Mul, x: Double, y: Double, rel) => rel.unify(x * y)
    case GoalNumOp2Double(NumOp2.Mul, x, y: Double, rel: Double) => x.unify(rel / y)
    case GoalNumOp2Double(NumOp2.Mul, x: Double, y, rel: Double) => y.unify(rel / x)
    case GoalNumOp2Double(_, _, _, _) => throw new IllegalArgumentException("not a 2-arg goal")
  }
}

implicit class GoalNumRangeOps(self: GoalNumRange) {

  import math.Ordered._

  private def check[T <: Num](num: T, low: Option[Boundary[VarOr[T]]], high: Option[Boundary[VarOr[T]]])(implicit order: Ordering[T]): Boolean =
    check0(num, low.asInstanceOf[Option[Boundary[T]]], high.asInstanceOf[Option[Boundary[T]]])

  private def check0[T <: Num](num: T, low: Option[Boundary[T]], high: Option[Boundary[T]])(implicit order: Ordering[T]): Boolean = {
    val lowOk = low match {
      case None => true
      case Some(Boundary(b, true)) => num >= b
      case Some(Boundary(b, false)) => num > b
    }
    val highOk = high match {
      case None => true
      case Some(Boundary(b, true)) => num <= b
      case Some(Boundary(b, false)) => num < b
    }
    lowOk && highOk
  }

  def guard(x: Boolean): Vector[Unifying[Option[GoalNumRange]]] = Vector(Unifying.guard(x) >> Unifying.success(None))

  def check: Vector[Unifying[Option[GoalNumRange]]] = self match {
    case GoalNumRangeByte(num: Byte, Some(low@Boundary(_: Byte, _)), Some(high@Boundary(_: Byte, _))) => guard(check(num, Some(low), Some(high)))
    case GoalNumRangeByte(num: Byte, None, Some(high@Boundary(_: Byte, _))) => guard(check(num, None, Some(high)))
    case GoalNumRangeByte(num: Byte, Some(low@Boundary(_: Byte, _)), None) => guard(check(num, Some(low), None))
    case GoalNumRangeShort(num: Short, Some(low@Boundary(_: Short, _)), Some(high@Boundary(_: Short, _))) => guard(check(num, Some(low), Some(high)))
    case GoalNumRangeShort(num: Short, None, Some(high@Boundary(_: Short, _))) => guard(check(num, None, Some(high)))
    case GoalNumRangeShort(num: Short, Some(low@Boundary(_: Short, _)), None) => guard(check(num, Some(low), None))
    case GoalNumRangeInt(num: Int, Some(low@Boundary(_: Int, _)), Some(high@Boundary(_: Int, _))) => guard(check(num, Some(low), Some(high)))
    case GoalNumRangeInt(num: Int, None, Some(high@Boundary(_: Int, _))) => guard(check(num, None, Some(high)))
    case GoalNumRangeInt(num: Int, Some(low@Boundary(_: Int, _)), None) => guard(check(num, Some(low), None))
    case GoalNumRangeLong(num: Long, Some(low@Boundary(_: Long, _)), Some(high@Boundary(_: Long, _))) => guard(check(num, Some(low), Some(high)))
    case GoalNumRangeLong(num: Long, None, Some(high@Boundary(_: Long, _))) => guard(check(num, None, Some(high)))
    case GoalNumRangeLong(num: Long, Some(low@Boundary(_: Long, _)), None) => guard(check(num, Some(low), None))
    case GoalNumRangeFloat(num: Float, Some(low@Boundary(_: Float, _)), Some(high@Boundary(_: Float, _))) => guard(check(num, Some(low), Some(high)))
    case GoalNumRangeFloat(num: Float, None, Some(high@Boundary(_: Float, _))) => guard(check(num, None, Some(high)))
    case GoalNumRangeFloat(num: Float, Some(low@Boundary(_: Float, _)), None) => guard(check(num, Some(low), None))
    case GoalNumRangeDouble(num: Double, Some(low@Boundary(_: Double, _)), Some(high@Boundary(_: Double, _))) => guard(check(num, Some(low), Some(high)))
    case GoalNumRangeDouble(num: Double, None, Some(high@Boundary(_: Double, _))) => guard(check(num, None, Some(high)))
    case GoalNumRangeDouble(num: Double, Some(low@Boundary(_: Double, _)), None) => guard(check(num, Some(low), None))
    // TODO: expand small ranges
    case x => Vector(Unifying.success(Some(x)))
  }
}

final case class NumState(op2s: Vector[GoalNumOp2], ranges: Vector[GoalNumRange]) {
  def insert(state: State, x: GoalNumOp2): IterableOnce[State] = copy(op2s = x +: op2s).onInsert(state)

  def insert(state: State, x: GoalNumRange): IterableOnce[State] = copy(ranges = x +: ranges).onInsert(state)

  def onEq(eq: EqState): IterableOnce[(EqState, NumState)] = for {
    (subst, ranges) <- NumState.runRanges(eq.subst, ranges)
    (subst, op2s) <- NumState.runOp2s(subst, op2s)
  } yield (EqState(subst), NumState(op2s = op2s, ranges = ranges))

  def onInsert(state: State): IterableOnce[State] = this.onEq(state.eq) map {
    case (eq, num) => state.copy(eq = eq, num = num)
  }

  def print: String = op2s.map(_.toString).mkString(" && ")
}

object NumState {
  def runOp2s(subst: Subst, op2s: Vector[GoalNumOp2]): Option[(Subst, Vector[GoalNumOp2])] = if (op2s.isEmpty) Some(subst, op2s) else // optimize
  {
    val (cl2, rest) = op2s.map(_.walk(subst)).partition(_.is2)
    Unifying.runAll(cl2.map(_.solve2)).getSubst(subst) map { subst =>
      (subst, rest)
    }
  }

  def runRanges(subst: Subst, ranges: Vector[GoalNumRange]): Option[(Subst, Vector[GoalNumRange])] = if (ranges.isEmpty) Some(subst, ranges) else // optimize
  {
    ???
  }

  val empty: NumState = NumState(Vector.empty, Vector.empty)
}
