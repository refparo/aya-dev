// Copyright (c) 2020-2020 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the Apache-2.0 license that can be found in the LICENSE file.
package org.mzi.concrete.resolve.visitor;

import org.glavo.kala.collection.mutable.Buffer;
import org.glavo.kala.control.Option;
import org.jetbrains.annotations.NotNull;
import org.mzi.concrete.Expr;
import org.mzi.concrete.Param;
import org.mzi.concrete.Stmt;
import org.mzi.concrete.resolve.context.Context;
import org.mzi.concrete.resolve.context.SimpleContext;
import org.mzi.concrete.visitor.ExprFixpoint;

/**
 * Resolves bindings.
 * @author re-xyr
 */
public final class ExprResolver implements ExprFixpoint<Context> {
  public static final @NotNull ExprResolver INSTANCE = new ExprResolver();

  private ExprResolver() {}

  @Override public @NotNull Expr visitUnresolved(@NotNull Expr.UnresolvedExpr expr, Context ctx) {
    return new Expr.RefExpr(expr.sourcePos(), Option.of(ctx.get(expr.name()))
      // TODO[xyr]: report instead of throw
      .getOrThrowException(new IllegalStateException("reference to non-existing variable `" + expr.name() + "`")));
  }

  public @NotNull Param visitParam(@NotNull Param param, Context ctx) {
    var var = param.var();
    var type = param.type();
    ctx.putLocal(var.name(), var, Stmt.Accessibility.Public);
    return new Param(param.sourcePos(), param.var(), type != null ? type.accept(this, ctx) : null, param.explicit());
  }

  @Override public @NotNull Buffer<@NotNull Param> visitParams(@NotNull Buffer<@NotNull Param> params, Context ctx) {
    return params.view().map(param -> {
      ctx.putLocal(param.var().name(), param.var(), Stmt.Accessibility.Public);
      var type = param.type();
      return new Param(param.sourcePos(), param.var(), type != null ? type.accept(this, ctx) : null, param.explicit());
    }).collect(Buffer.factory());
  }

  @Override public @NotNull Expr visitLam(@NotNull Expr.LamExpr expr, Context ctx) {
    var local = new SimpleContext();
    local.setOuterContext(ctx);
    var param = visitParams(Buffer.of(expr.param()), local).get(0);
    var body = expr.body().accept(this, local);
    return new Expr.LamExpr(expr.sourcePos(), param, body);
  }

  @Override public @NotNull Expr visitPi(@NotNull Expr.PiExpr expr, Context ctx) {
    var local = new SimpleContext();
    local.setOuterContext(ctx);
    var param = visitParams(Buffer.of(expr.param()), local).get(0);
    var last = expr.last().accept(this, local);
    return new Expr.PiExpr(expr.sourcePos(), expr.co(), param, last);
  }

  @Override public @NotNull Expr visitTelescopicSigma(@NotNull Expr.TelescopicSigmaExpr expr, Context ctx) {
    var local = new SimpleContext();
    local.setOuterContext(ctx);
    var params = visitParams(expr.params(), local).get(0);
    var last = expr.last().accept(this, local);
    return new Expr.PiExpr(expr.sourcePos(), expr.co(), params, last);
  }
}