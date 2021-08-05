// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
package org.aya.tyck.trace;

import kala.collection.mutable.Buffer;
import kala.tuple.Unit;
import org.aya.distill.CoreDistiller;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Style;
import org.jetbrains.annotations.NotNull;

public class MdUnicodeTrace implements Trace.Visitor<Unit, Doc> {
  public final int indent;
  public static final @NotNull Doc plus = Doc.symbol("+");
  public static final @NotNull Doc colon = Doc.symbol(":");

  public MdUnicodeTrace(int indent) {
    this.indent = indent;
  }

  public MdUnicodeTrace() {
    this(2);
  }

  @Override public Doc visitDecl(Trace.@NotNull DeclT t, Unit unit) {
    return Doc.vcat(Doc.sep(plus, CoreDistiller.varDoc(t.var())),
      indentedChildren(t.children()));
  }

  private @NotNull Doc indentedChildren(Buffer<@NotNull Trace> children) {
    return Doc.nest(indent, Doc.vcat(children.view().map(trace -> trace.accept(this, Unit.unit()))));
  }

  @Override public Doc visitExpr(Trace.@NotNull ExprT t, Unit unit) {
    var buf = Buffer.of(plus, Doc.symbol("\u22A2"), Doc.styled(Style.code(), t.expr().toDoc()));
    if (t.term() != null) {
      buf.append(colon);
      buf.append(t.term().toDoc());
    }
    return Doc.vcat(Doc.sep(buf), indentedChildren(t.children()));
  }

  @Override public Doc visitUnify(Trace.@NotNull UnifyT t, Unit unit) {
    var buf = Buffer.of(plus, Doc.symbol("\u22A2"), t.lhs().toDoc(), Doc.symbol("\u2261"), t.rhs().toDoc());
    if (t.type() != null) {
      buf.append(colon);
      buf.append(t.type().toDoc());
    }
    return Doc.vcat(Doc.sep(buf), indentedChildren(t.children()));
  }

  @Override public Doc visitTyck(Trace.@NotNull TyckT t, Unit unit) {
    assert t.children().isEmpty();
    return Doc.sep(plus, Doc.plain("result"), Doc.symbol("\u22A2"),
      Doc.styled(Style.code(), t.term().toDoc()), Doc.symbol("\u2191"),
      t.type().toDoc());
  }

  @Override public Doc visitPat(Trace.@NotNull PatT t, Unit unit) {
    return Doc.vcat(Doc.sep(plus, Doc.plain("pat"), Doc.symbol("\u22A2"),
        Doc.styled(Style.code(), t.pat().toDoc()), colon,
        t.term().toDoc()),
      indentedChildren(t.children()));
  }

  @Override public Doc visitLabel(Trace.@NotNull LabelT t, Unit unit) {
    return Doc.vcat(Doc.sep(plus, Doc.plain(t.label())), indentedChildren(t.children()));
  }

  public @NotNull Doc docify(Trace.@NotNull Builder traceBuilder) {
    return Doc.vcat(traceBuilder.root().view().map(e -> e.accept(this, Unit.unit())));
  }
}
