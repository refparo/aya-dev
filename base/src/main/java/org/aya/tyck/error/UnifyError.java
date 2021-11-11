// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.error;

import org.aya.api.distill.DistillerOptions;
import org.aya.api.error.ExprProblem;
import org.aya.api.util.NormalizeMode;
import org.aya.concrete.Expr;
import org.aya.core.term.Term;
import org.aya.pretty.doc.Doc;
import org.aya.tyck.TyckState;
import org.jetbrains.annotations.NotNull;

public record UnifyError(
  @Override @NotNull Expr expr,
  @NotNull Term expected,
  @NotNull Term actual
) implements ExprProblem {
  @Override public @NotNull Doc describe(@NotNull DistillerOptions options) {
    return Doc.vcat(
      Doc.english("Cannot check the expression of type"),
      Doc.par(1, actual.toDoc(options)),
      Doc.par(1, Doc.parened(Doc.sep(Doc.plain("Normalized:"), actual.normalize(TyckState.EMPTY, NormalizeMode.NF).toDoc(options)))),
      Doc.english("against the type"),
      Doc.par(1, expected.toDoc(options)),
      Doc.par(1, Doc.parened(Doc.sep(Doc.plain("Normalized:"), expected.normalize(TyckState.EMPTY, NormalizeMode.NF).toDoc(options))))
    );
  }

  @Override public @NotNull Severity level() {
    return Severity.ERROR;
  }
}
