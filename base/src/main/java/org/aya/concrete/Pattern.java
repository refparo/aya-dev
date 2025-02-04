// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.concrete;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import kala.value.MutableValue;
import org.aya.core.term.Term;
import org.aya.distill.BaseDistiller;
import org.aya.distill.ConcreteDistiller;
import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.ref.LocalVar;
import org.aya.ref.Var;
import org.aya.util.ForLSP;
import org.aya.util.binop.BinOpParser;
import org.aya.util.distill.DistillerOptions;
import org.aya.util.error.SourceNode;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author kiva, ice1000
 */
public sealed interface Pattern extends AyaDocile, SourceNode, BinOpParser.Elem<Pattern> {
  @Override default @NotNull Doc toDoc(@NotNull DistillerOptions options) {
    return new ConcreteDistiller(options).pattern(this, BaseDistiller.Outer.Free);
  }

  @Override @NotNull default Pattern expr() {
    return this;
  }

  record Tuple(
    @Override @NotNull SourcePos sourcePos,
    boolean explicit,
    @NotNull ImmutableSeq<Pattern> patterns,
    @Nullable LocalVar as
  ) implements Pattern {
  }

  record Number(
    @Override @NotNull SourcePos sourcePos,
    boolean explicit,
    int number
  ) implements Pattern {
  }

  record Absurd(
    @Override @NotNull SourcePos sourcePos,
    boolean explicit
  ) implements Pattern {
  }

  record CalmFace(
    @Override @NotNull SourcePos sourcePos,
    boolean explicit
  ) implements Pattern {
  }

  record Bind(
    @NotNull SourcePos sourcePos,
    boolean explicit,
    @NotNull LocalVar bind,
    @ForLSP @NotNull MutableValue<@Nullable Term> type
  ) implements Pattern {
  }

  record Ctor(
    @Override @NotNull SourcePos sourcePos,
    boolean explicit,
    @NotNull WithPos<@NotNull Var> resolved,
    @NotNull ImmutableSeq<Pattern> params,
    @Nullable LocalVar as
  ) implements Pattern {
    public Ctor(@NotNull Pattern.Bind bind, @NotNull Var maybe) {
      this(bind.sourcePos(), bind.explicit(), new WithPos<>(bind.sourcePos(), maybe), ImmutableSeq.empty(), null);
    }
  }

  record BinOpSeq(
    @NotNull SourcePos sourcePos,
    @NotNull ImmutableSeq<Pattern> seq,
    @Nullable LocalVar as,
    boolean explicit
  ) implements Pattern {}

  /**
   * @author kiva, ice1000
   */
  final class Clause {
    public final @NotNull SourcePos sourcePos;
    public final @NotNull ImmutableSeq<Pattern> patterns;
    public final @NotNull Option<Expr> expr;
    public boolean hasError = false;

    public Clause(@NotNull SourcePos sourcePos, @NotNull ImmutableSeq<Pattern> patterns, @NotNull Option<Expr> expr) {
      this.sourcePos = sourcePos;
      this.patterns = patterns;
      this.expr = expr;
    }
  }
}
