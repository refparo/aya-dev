// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.concrete.desugar;

import kala.collection.mutable.*;
import kala.value.Ref;
import org.aya.api.error.Reporter;
import org.aya.api.error.SourcePos;
import org.aya.api.util.Assoc;
import org.aya.concrete.desugar.error.OperatorProblem;
import org.aya.concrete.resolve.context.Context;
import org.aya.concrete.stmt.Command;
import org.aya.concrete.stmt.OpDecl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BinOpSet(
  @NotNull Reporter reporter,
  @NotNull MutableSet<BinOP> ops,
  @NotNull MutableHashMap<BinOP, MutableHashSet<BinOP>> tighterGraph
) {
  static final @NotNull BinOpSet.BinOP APP_ELEM = BinOP.from(SourcePos.NONE,
    () -> new OpDecl.Operator("application", Assoc.InfixL));

  public BinOpSet(@NotNull Reporter reporter) {
    this(reporter, MutableSet.of(APP_ELEM), MutableHashMap.of());
  }

  public void bind(@NotNull OpDecl op, @NotNull Command.BindPred pred, @NotNull OpDecl target, @NotNull SourcePos sourcePos) {
    var opElem = ensureHasElem(op, sourcePos);
    var targetElem = ensureHasElem(target, sourcePos);
    if (opElem == targetElem) {
      reporter.report(new OperatorProblem.BindSelfError(sourcePos));
      throw new Context.ResolvingInterruptedException();
    }
    switch (pred) {
      case Tighter -> addTighter(opElem, targetElem);
      case Looser -> addTighter(targetElem, opElem);
    }
  }

  public PredCmp compare(@NotNull BinOpSet.BinOP lhs, @NotNull BinOpSet.BinOP rhs) {
    // BinOp all have lower priority than application
    if (lhs == APP_ELEM) return PredCmp.Tighter;
    if (rhs == APP_ELEM) return PredCmp.Looser;
    if (lhs == rhs) return PredCmp.Equal;
    if (hasPath(MutableSet.of(), lhs, rhs)) return PredCmp.Tighter;
    if (hasPath(MutableSet.of(), rhs, lhs)) return PredCmp.Looser;
    return PredCmp.Undefined;
  }

  private boolean hasPath(@NotNull MutableSet<BinOP> book, @NotNull BinOpSet.BinOP from, @NotNull BinOpSet.BinOP to) {
    if (from == to) return true;
    if (book.contains(from)) return false;
    for (var test : ensureGraphHas(from)) {
      if (hasPath(book, test, to)) return true;
    }
    book.add(from);
    return false;
  }

  public Assoc assocOf(@Nullable OpDecl opDecl) {
    if (isOperand(opDecl)) return Assoc.Invalid;
    return ensureHasElem(opDecl).assoc;
  }

  public boolean isOperand(@Nullable OpDecl opDecl) {
    return opDecl == null || opDecl.asOperator() == null;
  }

  public BinOP ensureHasElem(@NotNull OpDecl opDecl) {
    return ensureHasElem(opDecl, SourcePos.NONE);
  }

  public BinOP ensureHasElem(@NotNull OpDecl opDecl, @NotNull SourcePos sourcePos) {
    var elem = ops.find(e -> e.op == opDecl);
    if (elem.isDefined()) return elem.get();
    var newElem = BinOP.from(sourcePos, opDecl);
    ops.add(newElem);
    return newElem;
  }

  private MutableHashSet<BinOP> ensureGraphHas(@NotNull BinOpSet.BinOP elem) {
    return tighterGraph.getOrPut(elem, MutableHashSet::of);
  }

  private void addTighter(@NotNull BinOpSet.BinOP from, @NotNull BinOpSet.BinOP to) {
    ensureGraphHas(to);
    ensureGraphHas(from).add(to);
  }

  public void sort() {
    var ind = MutableHashMap.<BinOP, Ref<Integer>>of();
    tighterGraph.forEach((from, tos) -> {
      ind.putIfAbsent(from, new Ref<>(0));
      tos.forEach(to -> ind.getOrPut(to, () -> new Ref<>(0)).value += 1);
    });

    var stack = LinkedBuffer.<BinOP>of();
    ind.forEach((e, i) -> {
      if (i.value == 0) stack.push(e);
    });

    var count = 0;
    while (stack.isNotEmpty()) {
      var elem = stack.pop();
      count += 1;
      tighterGraph.get(elem).forEach(to -> {
        if (--ind.get(to).value == 0) stack.push(to);
      });
    }

    if (count != tighterGraph.size()) {
      var circle = Buffer.<BinOP>create();
      ind.forEach((e, i) -> {
        if (i.value > 0) circle.append(e);
      });
      reporter.report(new OperatorProblem.CircleError(circle));
      throw new Context.ResolvingInterruptedException();
    }
  }

  public record BinOP(
    @NotNull SourcePos firstBind,
    @NotNull OpDecl op,
    @NotNull String name,
    @NotNull Assoc assoc
  ) {
    private static @NotNull OpDecl.Operator ensureOperator(@NotNull OpDecl opDecl) {
      var op = opDecl.asOperator();
      if (op == null) throw new IllegalArgumentException("not an operator");
      return op;
    }

    private static @NotNull BinOpSet.BinOP from(@NotNull SourcePos sourcePos, @NotNull OpDecl opDecl) {
      var op = ensureOperator(opDecl);
      return new BinOP(sourcePos, opDecl, op.name(), op.assoc());
    }
  }

  public enum PredCmp {
    Looser,
    Tighter,
    Undefined,
    Equal,
  }
}
