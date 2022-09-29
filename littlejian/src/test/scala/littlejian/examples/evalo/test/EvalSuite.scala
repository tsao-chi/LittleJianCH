package littlejian.examples.evalo.test

import littlejian.*
import littlejian.data.*
import littlejian.data.sexp.*
import littlejian.ext.*
import littlejian.search.naive.*
import littlejian.unifier.*
import littlejian.examples.evalo._

class EvalSuite extends munit.FunSuite {
  test("basics") {
    assertEquals(Set.from(run { lookupo(list(cons("a", "b")), "a") }), Set("b"))
    assertEquals(Set.from(run {
      lookupo(list(cons("a", "b"), cons("a", "c")), "a")
    }), Set("b"))
    assertEquals(Set.from(run {
      lookupo(list(cons("a", "b"), cons("a", "c")), "b")
    }), Set())
    assertEquals(Set.from(run {
      evalo(list(cons("a", "b"), cons("a", "c")), "a")
    }), Set("b"))
    assertEquals(Set.from(run {
      evalo(list(cons("a", "b"), cons("a", "c")), "b")
    }), Set())
    assertEquals(Set.from(run {
      val x: VarOr[SExp] = list("quote", "a")
      val result = hole[SExp]
      for {
        _ <- x === list("quote", result)
      } yield result
    }), Set("a"))
    assertEquals(Set.from(run {
      evalo(list(), list("quote", "a"))
    }), Set("a"))
    assertEquals(Set.from(run {
      evalo(list(), list("quote", "a", "b"))
    }), Set())
    assertEquals(Set.from(run {
      evalo(list(), list("quote", list("a")))
    }), Set("(a)"))
    assertEquals(Set.from(run {
      evalo((), list("list", list("quote", list("a"))))
    }), Set("((a))"))
    assertEquals(Set.from(run {
      evalo((), list("car", list("car", list("list", list("quote", list("a"))))))
    }), Set("a"))
  }
  test("What eval to a") {
    //assertEquals(run[SExp] { x => evalo((), x, "a") }.head, "")
  }
}