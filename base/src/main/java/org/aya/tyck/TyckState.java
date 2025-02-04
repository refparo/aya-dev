// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import kala.tuple.Unit;
import org.aya.core.Meta;
import org.aya.core.def.PrimDef;
import org.aya.core.term.CallTerm;
import org.aya.core.term.Term;
import org.aya.core.visitor.TermConsumer;
import org.aya.core.visitor.VarConsumer;
import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.tyck.env.LocalCtx;
import org.aya.tyck.error.HoleProblem;
import org.aya.tyck.trace.Trace;
import org.aya.tyck.unify.DefEq;
import org.aya.util.Ordering;
import org.aya.util.distill.DistillerOptions;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Currently we only deal with ambiguous equations (so no 'stuck' equations).
 */
public record TyckState(
  @NotNull MutableList<Eqn> eqns,
  @NotNull MutableList<WithPos<Meta>> activeMetas,
  @NotNull MutableMap<@NotNull Meta, @NotNull Term> metas,
  @NotNull PrimDef.Factory primFactory
) {
  public TyckState(@NotNull PrimDef.Factory primFactory) {
    this(MutableList.create(), MutableList.create(), MutableMap.create(), primFactory);
  }

  /**
   * @param trying whether to solve in a yasashi manner.
   */
  public void solveEqn(
    @NotNull Reporter reporter, Trace.@Nullable Builder tracer,
    @NotNull Eqn eqn, boolean trying
  ) {
    new DefEq(eqn.cmp, reporter, !trying, trying, tracer, this, eqn.pos, eqn.localCtx).checkEqn(eqn);
  }

  /** @return true if <code>this.eqns</code> and <code>this.activeMetas</code> are mutated. */
  public boolean simplify(
    @NotNull Reporter reporter, @Nullable Trace.Builder tracer
  ) {
    var removingMetas = MutableList.<WithPos<Meta>>create();
    for (var activeMeta : activeMetas) {
      if (metas.containsKey(activeMeta.data())) {
        eqns.filterInPlace(eqn -> {
          var usageCounter = new VarConsumer.UsageCounter(activeMeta.data());
          eqn.accept(usageCounter, Unit.unit());
          if (usageCounter.usageCount() > 0) {
            solveEqn(reporter, tracer, eqn, true);
            return false;
          } else return true;
        });
        removingMetas.append(activeMeta);
      }
    }
    activeMetas.filterNotInPlace(removingMetas::contains);
    return removingMetas.isNotEmpty();
  }

  public void solveMetas(@NotNull Reporter reporter, @Nullable Trace.Builder traceBuilder) {
    while (eqns.isNotEmpty()) {
      //noinspection StatementWithEmptyBody
      while (simplify(reporter, traceBuilder)) ;
      // If the standard 'pattern' fragment cannot solve all equations, try to use a nonstandard method
      var eqns = this.eqns.toImmutableSeq();
      if (eqns.isNotEmpty()) {
        for (var eqn : eqns) solveEqn(reporter, traceBuilder, eqn, false);
        reporter.report(new HoleProblem.CannotFindGeneralSolution(eqns));
      }
    }
  }

  public void addEqn(@NotNull Eqn eqn) {
    eqns.append(eqn);
    var currentActiveMetas = activeMetas.size();
    eqn.accept(new TermConsumer<>() {
      @Override public Unit visitHole(CallTerm.@NotNull Hole term, Unit unit) {
        var ref = term.ref();
        if (!metas.containsKey(ref))
          activeMetas.append(new WithPos<>(eqn.pos, ref));
        return unit;
      }
    }, Unit.unit());
    assert activeMetas.sizeGreaterThan(currentActiveMetas) : "Adding a bad equation";
  }

  public record Eqn(
    @NotNull Term lhs, @NotNull Term rhs,
    @NotNull Ordering cmp, @NotNull SourcePos pos,
    @NotNull LocalCtx localCtx,
    @NotNull DefEq.Sub lr, @NotNull DefEq.Sub rl
  ) implements AyaDocile {
    public <P, R> Tuple2<R, R> accept(@NotNull Term.Visitor<P, R> visitor, P p) {
      return Tuple.of(lhs.accept(visitor, p), rhs.accept(visitor, p));
    }

    public @NotNull Doc toDoc(@NotNull DistillerOptions options) {
      return Doc.stickySep(lhs.toDoc(options), Doc.symbol(cmp.symbol), rhs.toDoc(options));
    }
  }
}
