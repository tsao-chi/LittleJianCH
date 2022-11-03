package littlejian.examples.wasm

import littlejian._
import littlejian.ext._
import littlejian.data._

enum RuntimeValue derives Unify :
  case I32(x: VarOr[Int])
  case I64(x: VarOr[Long])
  case F32(x: VarOr[Float])
  case F64(x: VarOr[Double])
// TODO: 128

implicit class RuntimeValueOps(self: RuntimeValue) {
  def getType: Type = self match {
    case RuntimeValue.I32(_) => Type.I32
    case RuntimeValue.I64(_) => Type.I64
    case RuntimeValue.F32(_) => Type.F32
    case RuntimeValue.F64(_) => Type.F64
  }
}

implicit class VarOrRuntimeValueOps(self: VarOr[RuntimeValue]) {
  def force: GoalWith[RuntimeValue] = GoalWith(k => conde(
    for {
      x <- self.is[Int](RuntimeValue.I32(_))
      _ <- k(RuntimeValue.I32(x))
    } yield (),
    for {
      x <- self.is[Long](RuntimeValue.I64(_))
      _ <- k(RuntimeValue.I64(x))
    } yield (),
    for {
      x <- self.is[Float](RuntimeValue.F32(_))
      _ <- k(RuntimeValue.F32(x))
    } yield (),
    for {
      x <- self.is[Double](RuntimeValue.F64(_))
      _ <- k(RuntimeValue.F64(x))
    } yield (),
  ))

  def getType: Rel[Type] = force.map(_.getType)
}


