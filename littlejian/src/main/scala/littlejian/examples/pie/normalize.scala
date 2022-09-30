package littlejian.examples.pie
import littlejian._
import littlejian.ext._
import littlejian.data.sexp._

// https://github.com/bboskin/SFPW2018/blob/master/condp/normalize.rkt

/*
(defrel (normalizo Γ τ exp o)
  (fresh (v)
    (valofo Γ exp v)
    (read-backo Γ τ v o)))
*/
def normalizo(Γ: VarOr[SExp], τ: VarOr[SExp], exp: VarOr[SExp]): Rel[SExp] = for {
  v <- valofo(Γ, exp)
  o <- readBacko(Γ, τ, v)
} yield o

// Helpers for valofo

/*
(defrel (assign-simple tₑ tᵥ exp v)
  (== exp tₑ)
  (== v tᵥ))
*/
def assignSimple(te: VarOr[SExp], tv: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = te === exp && tv === v

/*
(defrel (valof-the ρ exp v)
  (fresh (τ e e-v t-v)
    (== exp `(the ,τ ,e))
    (== v `(THE ,t-v ,e-v))
    (valofo ρ e e-v)
    (valofo ρ τ t-v)))
*/
def valofThe(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  τ <- fresh[SExp]
  e <- fresh[SExp]
  ev <- fresh[SExp]
  tv <- fresh[SExp]
  _ <- exp === list("the", τ, e)
  _ <- v === list("THE", tv, ev)
  _ <- valofo(ρ, e, ev)
  _ <- valofo(ρ, τ, tv)
} yield ()

/*
(defrel (valof-neutral-var ρ exp v)
  (fresh (T)
    (== v `(NEU ,T (VAR ,exp)))
    (apply-Γ ρ exp T)))
*/
def valofNeutralVar(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  T <- fresh[SExp]
  _ <- v === list("NEU", T, list("VAR", exp))
  _ <- applyΓ(ρ, exp, T)
} yield ()

/*
(defrel (valof-quote ρ exp v)
  (fresh (atom)
    (== exp `(quote ,atom))
    (== v `(ATOM ,atom))))
*/
def valofQuote(ρ: VarOr[SExp], exp: VarOr[SExp]): Rel[SExp] = for {
  atom <- fresh[SExp]
  _ <- exp === list("quote", atom)
} yield list("ATOM", atom)
def valofQuote(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = valofQuote(ρ, exp)(v)

/*
(defrel (valof-Π ρ exp v)
  (fresh (x A D Ao)
    (== exp `(Π ([,x ,A]) ,D))
    (== v `(PI ,x ,Ao (CLOS ,ρ ,x ,D)))
    (valofo ρ A Ao)))
*/
def valofPi(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  x <- fresh[SExp]
  A <- fresh[SExp]
  D <- fresh[SExp]
  Ao <- fresh[SExp]
  _ <- exp === list("Π", list(list(x, A)), D)
  _ <- v === list("PI", x, Ao, list("CLOS", ρ, x, D))
  _ <- valofo(ρ, A, Ao)
} yield ()

/*
(defrel (valof-λ ρ exp v)
  (fresh (x b)
    (== exp `(λ (,x) ,b))
    (symbolo x)
    (== v `(LAM ,x (CLOS ,ρ ,x ,b)))))
*/
def valofLam(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  x <- fresh[SExp]
  b <- fresh[SExp]
  _ <- exp === list("λ", list(x), b)
  _ <- x.isType[String]
  _ <- v === list("LAM", x, list("CLOS", ρ, x, b))
} yield ()

/*
(defrel (valof-app ρ exp v)
  (fresh (rator rand rato rando)
    (== exp `(,rator ,rand))
    (valofo ρ rator rato)
    (valofo ρ rand rando)
    (do-appo rato rando v)))
*/
def valofApp(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  rator <- fresh[SExp]
  rand <- fresh[SExp]
  rato <- fresh[SExp]
  rando <- fresh[SExp]
  _ <- exp === list(rator, rand)
  _ <- valofo(ρ, rator, rato)
  _ <- valofo(ρ, rand, rando)
  _ <- doAppo(rato, rando, v)
} yield ()

/*
(defrel (valof-closuro clo v ans)
  (fresh (ρ x e ρ^)
    (== clo `(CLOS ,ρ ,x ,e))
    (extend-ρ ρ x v ρ^)
    (valofo ρ^ e ans)))
*/
def valofClosuro(clo: VarOr[SExp], v: VarOr[SExp], ans: VarOr[SExp]): Goal = for {
  ρ <- fresh[SExp]
  x <- fresh[SExp]
  e <- fresh[SExp]
  ρ_ <- fresh[SExp]
  _ <- clo === list("CLOS", ρ, x, e)
  _ <- extendρ(ρ, x, v, ρ_)
  _ <- valofo(ρ_, e, ans)
} yield ()

/*
(defrel (do-appo f v o)
  (conde
   [(fresh (x c)
      (== f `(LAM ,x ,c))
      (valof-closuro c v o))]
   [(fresh (x A c ne T)
           (== f `(NEU (PI ,x ,A ,c) ,ne))
           (== o `(NEU ,T (N-APP (NEU (PI ,x ,A ,c) ,ne) ,v)))
           (valof-closuro c v T))]))
*/
def doAppo(f: VarOr[SExp], v: VarOr[SExp], o: VarOr[SExp]): Goal = conde(
  for {
    x <- fresh[SExp]
    c <- fresh[SExp]
    _ <- f === list("LAM", x, c)
    _ <- valofClosuro(c, v, o)
  } yield (),
  for {
    x <- fresh[SExp]
    A <- fresh[SExp]
    c <- fresh[SExp]
    ne <- fresh[SExp]
    T <- fresh[SExp]
    _ <- f === list("NEU", list("PI", x, A, c), ne)
    _ <- o === list("NEU", T, list("N-APP", list("NEU", list("PI", x, A, c), ne), v))
    _ <- valofClosuro(c, v, T)
  } yield ()
)

/*
(defrel (valof-Σ ρ exp v)
  (fresh (x A D Ao)
    (== exp `(Σ ([,x ,A]) ,D))
    (== v `(SIGMA ,x ,Ao (CLOS ,ρ ,x ,D)))
    (valofo ρ A Ao)))
*/
def valofSigma(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  x <- fresh[SExp]
  A <- fresh[SExp]
  D <- fresh[SExp]
  Ao <- fresh[SExp]
  _ <- exp === list("Σ", list(list(x, A)), D)
  _ <- v === list("SIGMA", x, Ao, list("CLOS", ρ, x, D))
  _ <- valofo(ρ, A, Ao)
} yield ()

/*
(defrel (valof-cons ρ exp v)
  (fresh (a d a^ d^)
    (== exp `(cons ,a ,d))
    (== v `(CONS ,a^ ,d^))
    (valofo ρ a a^)
    (valofo ρ d d^)))
*/
def valofCons(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  a <- fresh[SExp]
  d <- fresh[SExp]
  a_ <- fresh[SExp]
  d_ <- fresh[SExp]
  _ <- exp === list("cons", a, d)
  _ <- v === list("CONS", a_, d_)
  _ <- valofo(ρ, a, a_)
  _ <- valofo(ρ, d, d_)
} yield ()

/*
(defrel (valof-car ρ exp v)
  (fresh (pr pr^)
    (== exp `(car ,pr))
    (do-caro pr^ v)
    (valofo ρ pr pr^)))
*/
def valofCar(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  pr <- fresh[SExp]
  pr_ <- fresh[SExp]
  _ <- exp === list("car", pr)
  _ <- doCaro(pr_, v)
  _ <- valofo(ρ, pr, pr_)
} yield ()

/*
(defrel (do-caro pr v)
  (conde
   [(fresh (a d)
           (== pr `(CONS ,a ,d))
           (== v a))]
   [(fresh (x A D ne)
           (== pr `(NEU (SIGMA ,x ,A ,D) ,ne))
           (== v `(NEU ,A (CAR (NEU (SIGMA ,x ,A ,D) ,ne)))))]))
*/
def doCaro(pr: VarOr[SExp], v: VarOr[SExp]): Goal = conde(
  for {
    a <- fresh[SExp]
    d <- fresh[SExp]
    _ <- pr === list("CONS", a, d)
    _ <- v === a
  } yield (),
  for {
    x <- fresh[SExp]
    A <- fresh[SExp]
    D <- fresh[SExp]
    ne <- fresh[SExp]
    _ <- pr === list("NEU", list("SIGMA", x, A, D), ne)
    _ <- v === list("NEU", A, list("CAR", list("NEU", list("SIGMA", x, A, D), ne)))
  } yield ()
)
/*
(defrel (valof-cdr ρ exp v)
  (fresh (pr pr^)
    (== exp `(cdr ,pr))
    (valofo ρ pr pr^)
    (do-cdro pr^ v)))
*/
def valofCdr(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  pr <- fresh[SExp]
  pr_ <- fresh[SExp]
  _ <- exp === list("cdr", pr)
  _ <- valofo(ρ, pr, pr_)
  _ <- doCdro(pr_, v)
} yield ()
/*
(defrel (do-cdro pr v)
  (conde
   [(fresh (a d)
           (== pr `(CONS ,a ,d))
           (== v d))]
   [(fresh (x A D D^ ne a)
           (== pr `(NEU (SIGMA ,x ,A ,D) ,ne))
           (do-caro pr a)
           (valof-closuro D a D^)
           (== v `(NEU ,D^ (CDR (NEU (SIGMA ,x ,A ,D) ,ne)))))]))
*/
def doCdro(pr: VarOr[SExp], v: VarOr[SExp]): Goal = conde(
  for {
    a <- fresh[SExp]
    d <- fresh[SExp]
    _ <- pr === list("CONS", a, d)
    _ <- v === d
  } yield (),
  for {
    x <- fresh[SExp]
    A <- fresh[SExp]
    D <- fresh[SExp]
    D_ <- fresh[SExp]
    ne <- fresh[SExp]
    a <- fresh[SExp]
    _ <- pr === list("NEU", list("SIGMA", x, A, D), ne)
    _ <- doCaro(pr, a)
    _ <- valofClosuro(D, a, D_)
    _ <- v === list("NEU", D_, list("CDR", list("NEU", list("SIGMA", x, A, D), ne)))
  } yield ()
)
/*
(defrel (valof-add1 ρ exp v)
  (fresh (n nV)
    (== exp `(add1 ,n))
    (== v `(ADD1 ,nV))
    (valofo ρ n nV)))
*/
def valofAdd1(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  n <- fresh[SExp]
  nV <- fresh[SExp]
  _ <- exp === list("add1", n)
  _ <- v === list("ADD1", nV)
  _ <- valofo(ρ, n, nV)
} yield ()
/*
(defrel (valof-ind-Nat ρ exp v)
  (fresh (t m τ ba s tV mV bV^ bV sV T)
    (== exp `(ind-Nat ,t ,m (the ,τ ,ba) ,s))
    (== bV `(THE ,T ,bV^))
    (valofo ρ t tV)
    (valofo ρ m mV)
    (valofo ρ ba bV^)
    (valofo ρ τ T)
    (valofo ρ s sV)
    (do-ind-Nat tV mV bV sV v)))
*/
def valofIndNat(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  t <- fresh[SExp]
  m <- fresh[SExp]
  τ <- fresh[SExp]
  ba <- fresh[SExp]
  s <- fresh[SExp]
  tV <- fresh[SExp]
  mV <- fresh[SExp]
  bV_ <- fresh[SExp]
  bV <- fresh[SExp]
  sV <- fresh[SExp]
  T <- fresh[SExp]
  _ <- exp === list("ind-Nat", t, m, list("the", τ, ba), s)
  _ <- bV === list("THE", T, bV_)
  _ <- valofo(ρ, t, tV)
  _ <- valofo(ρ, m, mV)
  _ <- valofo(ρ, ba, bV_)
  _ <- valofo(ρ, τ, T)
  _ <- valofo(ρ, s, sV)
  _ <- doIndNat(tV, mV, bV, sV, v)
} yield ()
/*
(defrel (do-ind-Nat t m b s o)
  (conde
   [(fresh (τ) (== t 'ZERO) (== b `(THE ,τ ,o)))]
   [(fresh (n res f^) (== t `(ADD1 ,n))
           (do-ind-Nat n m b s res)
           (do-appo s n f^)
           (do-appo f^ res o))]
   [(fresh (ne τ bas)
           (== t `(NEU NAT ,ne))
           (== o `(NEU ,τ (IND-NAT ,t ,m ,b ,s)))
           (do-appo m t τ))]))
*/
def doIndNat(t: VarOr[SExp], m: VarOr[SExp], b: VarOr[SExp], s: VarOr[SExp], o: VarOr[SExp]): Goal = conde(
  for {
    τ <- fresh[SExp]
    _ <- t === "ZERO"
    _ <- b === list("THE", τ, o)
  } yield (),
  for {
    n <- fresh[SExp]
    res <- fresh[SExp]
    f_ <- fresh[SExp]
    _ <- t === list("ADD1", n)
    _ <- doIndNat(n, m, b, s, res)
    _ <- doAppo(s, n, f_)
    _ <- doAppo(f_, res, o)
  } yield (),
  for {
    ne <- fresh[SExp]
    τ <- fresh[SExp]
    bas <- fresh[SExp]
    _ <- t === list("NEU", "NAT", ne)
    _ <- o === list("NEU", τ, list("IND-NAT", t, m, b, s))
    _ <- doAppo(m, t, τ)
  } yield ()
)
/*
(defrel (valof-= ρ exp v)
  (fresh (X from to Xv fromv tov)
    (== exp `(= ,X ,from ,to))
    (== v `(EQUAL ,Xv ,fromv ,tov))
    (valofo ρ X Xv)
    (valofo ρ from fromv)
    (valofo ρ to tov)))
*/
def valofEqual(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  X <- fresh[SExp]
  from <- fresh[SExp]
  to <- fresh[SExp]
  Xv <- fresh[SExp]
  fromv <- fresh[SExp]
  tov <- fresh[SExp]
  _ <- exp === list("=", X, from, to)
  _ <- v === list("EQUAL", Xv, fromv, tov)
  _ <- valofo(ρ, X, Xv)
  _ <- valofo(ρ, from, fromv)
  _ <- valofo(ρ, to, tov)
} yield ()
/*
(defrel (valof-same ρ exp v)
  (fresh (e eᵥ)
    (== exp `(same ,e))
    (== v `(SAME ,eᵥ))
    (valofo ρ e eᵥ)))
*/
def valofSame(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  e <- fresh[SExp]
  eV <- fresh[SExp]
  _ <- exp === list("same", e)
  _ <- v === list("SAME", eV)
  _ <- valofo(ρ, e, eV)
} yield ()
/*
(defrel (valof-ind-= ρ exp v)
  (fresh (t m b tV mV bV)
    (== exp `(ind-= ,t ,m ,b))
    (valofo ρ t tV)
    (valofo ρ m mV)
    (valofo ρ b bV)
    (do-ind-= ρ tV mV bV v)))
*/
def valofIndEqual(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = for {
  t <- fresh[SExp]
  m <- fresh[SExp]
  b <- fresh[SExp]
  tV <- fresh[SExp]
  mV <- fresh[SExp]
  bV <- fresh[SExp]
  _ <- exp === list("ind-=", t, m, b)
  _ <- valofo(ρ, t, tV)
  _ <- valofo(ρ, m, mV)
  _ <- valofo(ρ, b, bV)
  _ <- doIndEqual(ρ, tV, mV, bV, v)
} yield ()
/*
(defrel (do-ind-= ρ t m b o)
  (conde
   [(fresh (v f1 τ) (== t  `(SAME ,v))
           (== o b))]
   [(fresh (A from to ne f1 τ vars Tvar p Ao Fo To f2 τb)
      (== t `(NEU (EQUAL ,A ,from ,to) ,ne))
      (== o `(NEU ,τ
                  (IND-=
                   (NEU (EQUAL ,A ,from ,to) ,ne)
                   (THE (PI ,Tvar ,A (CLOS ,ρ ,Tvar (Π ([,p (= ,Ao ,Fo ,To)]) U))) ,m)
                   (THE ,τb ,b))))(do-appo m to f1)
      (just-names ρ vars)
      (freshen 'to vars Tvar)
      (freshen 'p vars p)
      (do-appo f1 t τ)
      (read-back-typo ρ A Ao)
      (read-backo ρ A from Fo)
      (read-backo ρ A to To)
      (do-appo m from f2)
      (do-appo f2 `(SAME ,from) τb))]))
*/
def doIndEqual(ρ: VarOr[SExp], t: VarOr[SExp], m: VarOr[SExp], b: VarOr[SExp], o: VarOr[SExp]): Goal = conde(
  for {
    v <- fresh[SExp]
    f1 <- fresh[SExp]
    τ <- fresh[SExp]
    _ <- t === list("SAME", v)
    _ <- o === b
  } yield (),
  for {
    A <- fresh[SExp]
    from <- fresh[SExp]
    to <- fresh[SExp]
    ne <- fresh[SExp]
    f1 <- fresh[SExp]
    τ <- fresh[SExp]
    vars <- fresh[SExp]
    Tvar <- fresh[SExp]
    p <- fresh[SExp]
    Ao <- fresh[SExp]
    Fo <- fresh[SExp]
    To <- fresh[SExp]
    f2 <- fresh[SExp]
    τb <- fresh[SExp]
    _ <- t === list("NEU", list("EQUAL", A, from, to), ne)
    _ <- o === list("NEU", τ, list("IND-=", list("NEU", list("EQUAL", A, from, to), ne), list("THE", list("PI", Tvar, A, list("CLOS", ρ, Tvar, list("Π", list(list(p, list("=", Ao, Fo, To))), "U"))), m), list("THE", τb, b)))
    _ <- doAppo(m, to, f1)
    _ <- justNames(ρ, vars)
    _ <- freshen("to", vars, Tvar)
    _ <- freshen("p", vars, p)
    _ <- doAppo(f1, t, τ)
    _ <- readBackTypo(ρ, A, Ao)
    _ <- readBacko(ρ, A, from, Fo)
    _ <- readBacko(ρ, A, to, To)
    _ <- doAppo(m, from, f2)
    _ <- doAppo(f2, list("SAME", from), τb)
  } yield ()
)
// relevance functions for valofo
/*
(define (valofo-in exp)
  (match exp
    [(? simple?) (list exp)]
    [(? symbol?) '(var)]
    [(? (exp-memv? non-symbol-exprs)) (list (car exp))]
    [`(,rat ,ran) '(app)]
    [(? var?) '(use-maybe)]))
*/
def valofoIn(exp: VarOr[SExp])(walker: Walker): Seq[String] = walkStar(walker, exp) match {
  case exp: String if simpleQ(exp) => Seq(exp)
  case _: String => Seq("var")
  case exp if expMemvQ(nonSymbolExprs)(exp) => Seq(car(exp).asInstanceOf[String])
  case Cons(rat, ran) => Seq("app")
  case _: Var[_] => Seq(UseMaybe)
}

def valofo(ρ: VarOr[SExp], exp: VarOr[SExp]): Rel[SExp] = valofo(ρ, exp, _)
def valofo(ρ: VarOr[SExp], exp: VarOr[SExp], v: VarOr[SExp]): Goal = ???
def readBacko(Γ: VarOr[SExp], τ: VarOr[SExp], v: VarOr[SExp]): Rel[SExp] = readBacko(Γ, τ, v, _)
def readBacko(Γ: VarOr[SExp], τ: VarOr[SExp], v: VarOr[SExp], norm: VarOr[SExp]): Goal = ???
def readBackTypo(Γ: VarOr[SExp], v: VarOr[SExp], norm: VarOr[SExp]): Goal = ???