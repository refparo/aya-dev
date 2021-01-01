// Copyright (c) 2020-2020 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the Apache-2.0 license that can be found in the LICENSE file.
package org.mzi.concrete;

import asia.kala.Tuple;
import asia.kala.Tuple2;
import asia.kala.collection.immutable.ImmutableSeq;
import asia.kala.collection.immutable.ImmutableVector;
import asia.kala.collection.mutable.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mzi.api.error.SourcePos;
import org.mzi.api.ref.Var;
import org.mzi.generic.Arg;

import java.util.stream.Stream;

/**
 * @author re-xyr
 */
public sealed interface Expr {
  <P, R> R accept(@NotNull Visitor<P, R> visitor, P p);

  @NotNull SourcePos sourcePos();

  interface Visitor<P, R> {
    R visitRef(@NotNull RefExpr expr, P p);
    R visitUnresolved(@NotNull UnresolvedExpr expr, P p);
    R visitLam(@NotNull LamExpr expr, P p);
    R visitPi(@NotNull Expr.PiExpr expr, P p);
    R visitTelescopicLam(@NotNull TelescopicLamExpr expr, P p);
    R visitTelescopicPi(@NotNull Expr.TelescopicPiExpr expr, P p);
    R visitTelescopicSigma(@NotNull Expr.TelescopicSigmaExpr expr, P p);
    R visitUniv(@NotNull UnivExpr expr, P p);
    R visitApp(@NotNull AppExpr expr, P p);
    R visitHole(@NotNull HoleExpr expr, P p);
    R visitTup(@NotNull TupExpr expr, P p);
    R visitProj(@NotNull ProjExpr expr, P p);
    R visitTyped(@NotNull TypedExpr expr, P p);
    R visitLitInt(@NotNull LitIntExpr expr, P p);
    R visitLitString(@NotNull LitStringExpr expr, P p);
  }

  interface BaseVisitor<P, R> extends Visitor<P, R> {
    R catchAll(@NotNull Expr expr, P p);
    @Override default R visitRef(@NotNull RefExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitUnresolved(@NotNull UnresolvedExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitTelescopicLam(@NotNull Expr.TelescopicLamExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitTelescopicPi(@NotNull Expr.TelescopicPiExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitTelescopicSigma(@NotNull Expr.TelescopicSigmaExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitUniv(@NotNull UnivExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitApp(@NotNull AppExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitHole(@NotNull HoleExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitTup(@NotNull TupExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitProj(@NotNull ProjExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitTyped(@NotNull TypedExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitLitInt(@NotNull LitIntExpr expr, P p) {
      return catchAll(expr, p);
    }
    @Override default R visitLitString(@NotNull LitStringExpr expr, P p) {
      return catchAll(expr, p);
    }
  }

  /**
   * @author re-xyr
   */
  record UnresolvedExpr(
    @NotNull SourcePos sourcePos,
    @NotNull String name
  ) implements Expr {
    @Override
    public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitUnresolved(this, p);
    }
  }

  /**
   * @author ice1000
   */
  record HoleExpr(
    @NotNull SourcePos sourcePos,
    @Nullable String name,
    @Nullable Expr filling
  ) implements Expr {
    @Override
    public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitHole(this, p);
    }
  }

  /**
   * @author ice1000
   */
  interface TelescopicExpr {
    @NotNull Buffer<Param> params();

    default @NotNull Stream<@NotNull Tuple2<@NotNull Var, Param>> paramsStream() {
      return params().stream().map(p -> Tuple.of(p.var(), p));
    }
  }

  /**
   * @author re-xyr
   */
  record AppExpr(
    @NotNull SourcePos sourcePos,
    @NotNull Expr function,
    @NotNull ImmutableSeq<@NotNull Arg<Expr>> argument
  ) implements Expr {
    @Override
    public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitApp(this, p);
    }
  }

  /**
   * @author re-xyr
   */
  record PiExpr(
    @NotNull SourcePos sourcePos,
    boolean co,
    @NotNull Param param,
    @NotNull Expr last
  ) implements Expr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitPi(this, p);
    }
  }

  /**
   * @author re-xyr, kiva
   */
  record TelescopicPiExpr(
    @NotNull SourcePos sourcePos,
    boolean co,
    @NotNull Buffer<Param> params,
    @NotNull Expr last
  ) implements Expr, TelescopicExpr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitTelescopicPi(this, p);
    }
  }

  /**
   * @author re-xyr
   */
  record LamExpr(
    @NotNull SourcePos sourcePos,
    @NotNull Param param,
    @NotNull Expr body
  ) implements Expr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitLam(this, p);
    }
  }

  /**
   * @author re-xyr
   */
  record TelescopicLamExpr(
    @NotNull SourcePos sourcePos,
    @NotNull Buffer<@NotNull Param> params,
    @NotNull Expr body
  ) implements Expr, TelescopicExpr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitTelescopicLam(this, p);
    }
  }

  /**
   * @author re-xyr
   */
  record TelescopicSigmaExpr(
    @NotNull SourcePos sourcePos,
    boolean co,
    @NotNull Buffer<@NotNull Param> params,
    @NotNull Expr last
  ) implements Expr, TelescopicExpr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitTelescopicSigma(this, p);
    }
  }

  /**
   * @author ice1000
   */
  record RefExpr(
    @NotNull SourcePos sourcePos,
    @NotNull Var resolvedVar
  ) implements Expr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitRef(this, p);
    }
  }

  /**
   * @param hLevel specified hLevel, <= -3 if not specified.
   * @param uLevel specified uLevel, <= -3 if not specified.
   * @author re-xyr, ice1000
   */
  record UnivExpr(
    @NotNull SourcePos sourcePos,
    int uLevel,
    int hLevel
  ) implements Expr {
    public UnivExpr(@NotNull SourcePos sourcePos) {
      // TODO[level]: initialize the levels
      this(sourcePos, -3, -3);
    }

    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitUniv(this, p);
    }
  }

  /**
   * @author re-xyr
   */
  record TupExpr(
    @NotNull SourcePos sourcePos,
    @NotNull ImmutableVector<@NotNull Expr> items
  ) implements Expr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitTup(this, p);
    }
  }

  /**
   * @author re-xyr
   */
  record ProjExpr(
    @NotNull SourcePos sourcePos,
    @NotNull Expr tup,
    int ix
  ) implements Expr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitProj(this, p);
    }
  }

  /**
   * @author kiva
   */
  record TypedExpr(
    @NotNull SourcePos sourcePos,
    @NotNull Expr expr,
    @NotNull Expr type
  ) implements Expr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitTyped(this, p);
    }
  }

  /**
   * @author kiva
   */
  record LitIntExpr(
    @NotNull SourcePos sourcePos,
    int integer
  ) implements Expr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitLitInt(this, p);
    }
  }

  record LitStringExpr(
    @NotNull SourcePos sourcePos,
    @NotNull String string
  ) implements Expr {
    @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
      return visitor.visitLitString(this, p);
    }
  }
}
