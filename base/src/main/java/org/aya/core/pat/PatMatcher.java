// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.core.pat;

import kala.collection.SeqLike;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableHashMap;
import kala.control.Result;
import kala.tuple.Tuple2;
import org.aya.api.util.Arg;
import org.aya.core.def.PrimDef;
import org.aya.core.term.CallTerm;
import org.aya.core.term.IntroTerm;
import org.aya.core.term.Term;
import org.aya.core.visitor.Substituter;
import org.jetbrains.annotations.NotNull;

/**
 * Matches a term with a pattern.
 *
 * @author ice1000
 * @apiNote Use {@link PatMatcher#tryBuildSubstArgs(ImmutableSeq, SeqLike)} instead of instantiating the class directly.
 * @implNote The substitution built is made from parallel substitutions.
 */
public record PatMatcher(@NotNull Substituter.TermSubst subst) {
  /**
   * @return ok if the term matches the pattern,
   * err(false) if fails positively, err(true) if fails negatively
   */
  public static Result<Substituter.TermSubst, Boolean> tryBuildSubstArgs(
    @NotNull ImmutableSeq<@NotNull Pat> pats,
    @NotNull SeqLike<@NotNull Arg<@NotNull Term>> terms
  ) {
    return tryBuildSubstTerms(pats, terms.view().map(Arg::term));
  }

  /** @see this#tryBuildSubstArgs(ImmutableSeq, SeqLike) */
  public static Result<Substituter.TermSubst, Boolean> tryBuildSubstTerms(
    @NotNull ImmutableSeq<@NotNull Pat> pats,
    @NotNull SeqView<@NotNull Term> terms
  ) {
    var matchy = new PatMatcher(new Substituter.TermSubst(new MutableHashMap<>()));
    try {
      for (var pat : pats.zip(terms)) matchy.match(pat);
      return Result.ok(matchy.subst());
    } catch (Mismatch mismatch) {
      return Result.err(mismatch.isBlocked);
    }
  }

  private void match(@NotNull Pat pat, @NotNull Term term) throws Mismatch {
    switch (pat) {
      case Pat.Bind bind -> subst.addDirectly(bind.as(), term);
      case Pat.Absurd absurd -> throw new IllegalStateException("unreachable");
      case Pat.Prim prim -> {
        var core = prim.ref().core;
        assert PrimDef.Factory.INSTANCE.leftOrRight(core);
        if (!(term instanceof CallTerm.Prim primCall)) throw new Mismatch(true);
        if (primCall.ref() != prim.ref()) throw new Mismatch(false);
      }
      case Pat.Ctor ctor -> {
        if (!(term instanceof CallTerm.Con conCall)) throw new Mismatch(true);
        var as = ctor.as();
        if (as != null) subst.addDirectly(as, conCall);
        if (ctor.ref() != conCall.ref()) throw new Mismatch(false);
        visitList(ctor.params(), conCall.conArgs().view().map(Arg::term));
      }
      case Pat.Tuple tuple -> {
        if (!(term instanceof IntroTerm.Tuple tup)) throw new Mismatch(true);
        var as = tuple.as();
        if (as != null) subst.addDirectly(as, tup);
        visitList(tuple.pats(), tup.items());
      }
      case Pat.Meta meta -> {
        var sol = meta.solution().value;
        assert sol != null : "Unsolved pattern " + meta;
        match(sol, term);
      }
    }
  }

  private void visitList(ImmutableSeq<Pat> lpats, SeqLike<Term> terms) throws Mismatch {
    assert lpats.sizeEquals(terms);
    lpats.view().zip(terms).forEachChecked(this::match);
  }

  private void match(@NotNull Tuple2<Pat, Term> pp) throws Mismatch {
    match(pp._1, pp._2);
  }

  private static final class Mismatch extends Exception {
    public final boolean isBlocked;

    private Mismatch(boolean isBlocked) {
      this.isBlocked = isBlocked;
    }
  }
}
