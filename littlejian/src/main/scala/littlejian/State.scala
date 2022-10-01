package littlejian

import scala.collection.parallel.immutable.{ParHashMap, ParSeq, ParVector}
import collection.parallel.CollectionConverters._

final case class EqState(subst: Subst)

object EqState {
  val empty: EqState = EqState(Subst.empty)
}

sealed class NotEqRequest[T](val x: VarOr[T], val y: VarOr[T], val unifier: Unifier[T])

private def NotEqRequestUnchecked[T, U](x: T, y: U, unifier: Unifier[_]): NotEqRequest[_] =
  new NotEqRequest[T | U](x, y, unifier.asInstanceOf[Unifier[T | U]])

final case class NotEqElem[T](override val x: Var[T], override val y: VarOr[T], override val unifier: Unifier[T]) extends NotEqRequest[T](x, y, unifier)

final case class NotEqState(clauses: ParVector /*conj*/ [ParVector[NotEqElem[_]] /*disj not eq*/ ]) {
  def onEq(eq: EqState): Option[NotEqState] =
    if (clauses.isEmpty) Some(this) else // optimize
      NotEqState.check(eq, clauses)
}

object NotEqState {
  val empty: NotEqState = NotEqState(ParVector.empty)

  import littlejian.utils._

  private def exec[T](eq: EqState, req: NotEqRequest[T]): Option[ParVector /*disj, empty means success*/ [NotEqElem[_]]] = (eq.subst.walk(req.x), eq.subst.walk(req.y)) match {
    case (x: Var[T], y: Var[T]) if x == y => None
    case (x: Var[T], y) => Some(ParVector(NotEqElem(x, y, req.unifier)))
    case (x, y: Var[T]) => Some(ParVector(NotEqElem(y, x, req.unifier)))
    case (x, y) => req.unifier.unify(x, y)(Subst.empty) match {
      case None => Some(ParVector.empty)
      case Some((newSubst, ())) => if (newSubst.isEmpty) None else runCheck(eq, newSubst.toSeq.par.map({ case (v, (unifier, x)) => NotEqRequestUnchecked(v, x, unifier) }))
    }
  }

  private def runCheck(eq: EqState, x: ParSeq /*disj*/ [NotEqRequest[_]]): Option[ParVector /*disj, empty means success*/ [NotEqElem[_]]] =
    if (x.isEmpty) throw new IllegalArgumentException("Empty vector")
    else {
      val result = x.map(exec(eq, _)).filter(_.isDefined).map(_.get)
      if (result.isEmpty) None
      else if (result.exists(_.isEmpty)) Some(ParVector.empty)
      else Some(result.fold(ParVector.empty)(_ ++ _))
    }

  private def check(eq: EqState, xs: ParVector[ParVector[NotEqRequest[_]]]): Option[NotEqState] =
    traverse(xs.map(runCheck(eq, _))).map(xs => NotEqState(xs.filter(_.nonEmpty)))

  private[littlejian] def create(eq: EqState, req: NotEqRequest[_], clauses: NotEqState): Option[NotEqState] = check(eq, ParVector(req) +: clauses.clauses)
}

final case class PredTypeState(xs: ParVector[(Var[_], PredTypeTag)]) {
  def insert(v: Var[_], t: PredTypeTag): PredTypeState = PredTypeState((v, t) +: xs)

  def onEq(eq: EqState): Option[PredTypeState] = {
    if (xs.isEmpty) return Some(this) // optimize
    val (bound0, rest) = xs.partition(x => eq.subst.contains(x._1))
    val bound = bound0.map(x => (eq.subst.walk(x._1), x._2))
    val (vars, concretes) = bound.partition(x => x._1 match {
      case _: Var[_] => true
      case _ => false
    })
    if (concretes.forall(x => checkPredTypeTag(x._2, x._1))) Some(PredTypeState(vars.map(x => (x._1.asInstanceOf[Var[_]], x._2)) ++ rest)) else None
  }
}

object PredTypeState {
  val empty: PredTypeState = PredTypeState(ParVector.empty)
}

final case class PredNotTypeState(xs: ParVector[(Var[_], PredTypeTag)]) {
  def insert(v: Var[_], t: PredTypeTag): PredNotTypeState = PredNotTypeState((v, t) +: xs)

  def onEq(eq: EqState): Option[PredNotTypeState] = {
    if (xs.isEmpty) return Some(this) // optimize
    val (bound0, rest) = xs.partition(x => eq.subst.contains(x._1))
    val bound = bound0.map(x => (eq.subst.walk(x._1), x._2))
    val (vars, concretes) = bound.partition(x => x._1 match {
      case _: Var[_] => true
      case _ => false
    })
    if (concretes.forall(x => !checkPredTypeTag(x._2, x._1))) Some(PredNotTypeState(vars.map(x => (x._1.asInstanceOf[Var[_]], x._2)) ++ rest)) else None
  }
}

object PredNotTypeState {
  val empty: PredNotTypeState = PredNotTypeState(ParVector.empty)
}

final case class AbsentState(absents: ParVector/*conj*/[(Any, ParVector/*disj*/[WithInspector[_]])]) {
  def insert(eq: EqState, goal: GoalAbsent[_]): Option[AbsentState] = Inspector.scanUncertain(goal.x, eq.subst.walk, goal.absent) match {
    case None => None
    case Some(xs) => if(xs.isEmpty) Some(this) else Some(AbsentState((goal.absent, Vector.from(xs).par) +: absents))
  }

  def onEq(eq: EqState): Option[AbsentState] = AbsentState.check(eq, absents)
}

object AbsentState {
  val empty: AbsentState = AbsentState(ParVector.empty)

  import littlejian.utils._

  def check(eq: EqState, absents: ParVector/*conj*/[(Any, ParVector/*disj*/[WithInspector[_]])]): Option[AbsentState] =
    if(absents.isEmpty) Some(AbsentState.empty) // optimize
    else {
      traverse(absents.map({case (v, xs) => for {
        ys <- Inspector.scanUncertain(xs, eq.subst.walk, v)
      } yield (v, ys)})).map(
        next => AbsentState(next.filter({case (_, xs) => xs.nonEmpty}))
      )
    }
}

final case class State(eq: EqState, notEq: NotEqState, predType: PredTypeState, predNotType: PredNotTypeState, absent: AbsentState) {
  def eqUpdated(eq: EqState): State = State(eq = eq, notEq = notEq, predType = predType, predNotType = predNotType, absent = absent)

  def notEqUpdated(notEq: NotEqState): State = State(eq = eq, notEq = notEq, predType = predType, predNotType = predNotType, absent = absent)

  def predTypeUpdated(predType: PredTypeState): State = State(eq = eq, notEq = notEq, predType = predType, predNotType = predNotType, absent = absent)

  def predNotTypeUpdated(predNotType: PredNotTypeState): State = State(eq = eq, notEq = notEq, predType = predType, predNotType = predNotType, absent = absent)

  def absentUpdated(absent: AbsentState): State = State(eq = eq, notEq = notEq, predType = predType, predNotType = predNotType, absent = absent)

  def predTypeMap(f: PredTypeState => PredTypeState): State = predTypeUpdated(f(predType))

  def predNotTypeMap(f: PredNotTypeState => PredNotTypeState): State = predNotTypeUpdated(f(predNotType))

  // Update Constraints
  def onEq: Option[State] = for {
    notEq <- notEq.onEq(eq)
    predType <- predType.onEq(eq)
    predNotType <- predNotType.onEq(eq)
    absent <- absent.onEq(eq)
  } yield State(eq = eq, notEq = notEq, predType = predType, predNotType = predNotType, absent = absent)

  def setEq(eq: EqState) = this.eqUpdated(eq).onEq
}

object State {
  val empty: State = State(eq = EqState.empty, notEq = NotEqState.empty, predType = PredTypeState.empty, predNotType = PredNotTypeState.empty, absent = AbsentState.empty)
}