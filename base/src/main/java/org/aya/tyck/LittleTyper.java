// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Unit;
import org.aya.api.ref.DefVar;
import org.aya.api.util.NormalizeMode;
import org.aya.concrete.stmt.Decl;
import org.aya.core.def.Def;
import org.aya.core.sort.LevelSubst;
import org.aya.core.sort.Sort;
import org.aya.core.term.*;
import org.aya.core.visitor.Substituter;
import org.aya.core.visitor.Unfolder;
import org.aya.generic.Constants;
import org.jetbrains.annotations.NotNull;

/**
 * Similar to <code>GetTypeVisitor</code> in Arend.
 *
 * @author ice1000
 */
public record LittleTyper(@NotNull TyckState state) implements Term.Visitor<Unit, Term> {
  @Override public Term visitRef(@NotNull RefTerm term, Unit unit) {
    return term.type();
  }

  @Override public Term visitLam(IntroTerm.@NotNull Lambda term, Unit unit) {
    return new FormTerm.Pi(term.param(), term.body().accept(this, unit));
  }

  @Override public Term visitPi(FormTerm.@NotNull Pi term, Unit unit) {
    var paramTyRaw = term.param().type().accept(this, Unit.unit()).normalize(state, NormalizeMode.WHNF);
    var retTyRaw = term.body().accept(this, Unit.unit()).normalize(state, NormalizeMode.WHNF);
    if (paramTyRaw instanceof FormTerm.Univ paramTy && retTyRaw instanceof FormTerm.Univ retTy)
      return new FormTerm.Univ(Sort.max(paramTy.sort(), retTy.sort()));
    else return ErrorTerm.typeOf(term);
  }

  @Override public Term visitError(@NotNull ErrorTerm term, Unit unit) {
    return ErrorTerm.typeOf(term);
  }

  @Override public Term visitSigma(FormTerm.@NotNull Sigma term, Unit unit) {
    var univ = term.params().view()
      .map(param -> param.type()
        .accept(this, Unit.unit()).normalize(state, NormalizeMode.WHNF))
      .filterIsInstance(FormTerm.Univ.class)
      .toImmutableSeq();
    if (univ.sizeEquals(term.params().size()))
      return new FormTerm.Univ(univ.view().map(FormTerm.Univ::sort).reduce(Sort::max));
    else return ErrorTerm.typeOf(term);
  }

  @Override public Term visitUniv(FormTerm.@NotNull Univ term, Unit unit) {
    return new FormTerm.Univ(term.sort().lift(1));
  }

  @Override public Term visitApp(ElimTerm.@NotNull App term, Unit unit) {
    var piRaw = term.of().accept(this, unit).normalize(state, NormalizeMode.WHNF);
    return piRaw instanceof FormTerm.Pi pi ? pi.substBody(term.arg().term(), state) : ErrorTerm.typeOf(term);
  }

  @Override public Term visitFnCall(@NotNull CallTerm.Fn fnCall, Unit unit) {
    return defCall(fnCall.ref(), fnCall.sortArgs());
  }

  @Override public Term visitDataCall(@NotNull CallTerm.Data dataCall, Unit unit) {
    return defCall(dataCall.ref(), dataCall.sortArgs());
  }

  @Override public Term visitConCall(@NotNull CallTerm.Con conCall, Unit unit) {
    return defCall(conCall.head().dataRef(), conCall.sortArgs());
  }

  @Override public Term visitStructCall(@NotNull CallTerm.Struct structCall, Unit unit) {
    return defCall(structCall.ref(), structCall.sortArgs());
  }

  @NotNull
  private Term defCall(DefVar<? extends Def, ? extends Decl> ref, ImmutableSeq<@NotNull Sort> sortArgs) {
    var levels = Def.defLevels(ref);
    return Def.defResult(ref).subst(Substituter.TermSubst.EMPTY, Unfolder.buildSubst(levels, sortArgs));
  }

  @Override public Term visitPrimCall(CallTerm.@NotNull Prim prim, Unit unit) {
    return defCall(prim.ref(), prim.sortArgs());
  }

  @Override public Term visitTup(IntroTerm.@NotNull Tuple term, Unit unit) {
    return new FormTerm.Sigma(term.items().map(item ->
      new Term.Param(Constants.anonymous(), item.accept(this, Unit.unit()), true)));
  }

  @Override public Term visitNew(IntroTerm.@NotNull New newTerm, Unit unit) {
    return newTerm.struct();
  }

  @Override public Term visitProj(ElimTerm.@NotNull Proj term, Unit unit) {
    var sigmaRaw = term.of().accept(this, unit).normalize(state, NormalizeMode.WHNF);
    if (!(sigmaRaw instanceof FormTerm.Sigma sigma)) return ErrorTerm.typeOf(term);
    var index = term.ix() - 1;
    var telescope = sigma.params();
    return telescope.get(index).type()
      .subst(ElimTerm.Proj.projSubst(term.of(), index, telescope, state), LevelSubst.EMPTY);
  }

  @Override public Term visitAccess(CallTerm.@NotNull Access term, Unit unit) {
    var callRaw = term.of().accept(this, unit).normalize(state, NormalizeMode.WHNF);
    if (!(callRaw instanceof CallTerm.Struct call)) return ErrorTerm.typeOf(term);
    var core = term.ref().core;
    var subst = Unfolder.buildSubst(state, core.telescope(), term.fieldArgs())
      .add(Unfolder.buildSubst(state, call.ref().core.telescope(), term.structArgs()));
    return core.result().subst(subst, LevelSubst.EMPTY);
  }

  @Override public Term visitHole(CallTerm.@NotNull Hole term, Unit unit) {
    return term.ref().result;
  }

  @Override
  public Term visitFieldRef(@NotNull RefTerm.Field term, Unit unit) {
    return Def.defType(term.ref());
  }
}
