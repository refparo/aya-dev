// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.core.def;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.aya.concrete.stmt.TeleDecl;
import org.aya.core.Matching;
import org.aya.core.term.Term;
import org.aya.generic.Modifier;
import org.aya.ref.DefVar;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.function.BiFunction;

/**
 * @author ice1000
 */
public final class FnDef extends UserDef {
  public final @NotNull EnumSet<Modifier> modifiers;
  public final @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref;
  public final @NotNull Either<Term, ImmutableSeq<Matching>> body;

  public FnDef(
    @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref, @NotNull ImmutableSeq<Term.Param> telescope,
    @NotNull Term result,
    @NotNull EnumSet<Modifier> modifiers,
    @NotNull Either<Term, ImmutableSeq<Matching>> body
  ) {
    super(telescope, result);
    this.modifiers = modifiers;
    ref.core = this;
    this.ref = ref;
    this.body = body;
  }

  public static <T> BiFunction<Term, Either<Term, ImmutableSeq<Matching>>, T>
  factory(BiFunction<Term, Either<Term, ImmutableSeq<Matching>>, T> function) {
    return function;
  }

  @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
    return visitor.visitFn(this, p);
  }

  public @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref() {
    return ref;
  }
}
