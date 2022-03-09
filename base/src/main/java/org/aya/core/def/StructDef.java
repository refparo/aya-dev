// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.concrete.stmt.Decl;
import org.aya.core.sort.Sort;
import org.aya.core.term.CallTerm;
import org.aya.core.term.Term;
import org.aya.generic.Arg;
import org.aya.ref.DefVar;
import org.jetbrains.annotations.NotNull;

/**
 * core struct definition, corresponding to {@link Decl.StructDecl}
 *
 * @author vont
 */

public final class StructDef extends UserDef.Type {

  public final @NotNull DefVar<StructDef, Decl.StructDecl> ref;
  public final @NotNull ImmutableSeq<FieldDef> fields;
  public final @NotNull ImmutableSeq<CallTerm.Struct> parents;

  public StructDef(
    @NotNull DefVar<StructDef, Decl.StructDecl> ref,
    @NotNull ImmutableSeq<Term.Param> telescope,
    @NotNull ImmutableSeq<Sort.LvlVar> levels,
    @NotNull Sort sort,
    @NotNull ImmutableSeq<FieldDef> fields,
    @NotNull ImmutableSeq<CallTerm.Struct> parents) {
    super(telescope, sort, levels);
    parents.forEach(parent -> {
      parent.ref().core.fields.forEach(field -> {
        assert fields.contains(field);
      });
      assert parent.ref().core.telescope().size() == parent.args().size();
    });
    this.parents = parents;
    ref.core = this;
    this.ref = ref;
    this.fields = fields;
  }

  @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
    return visitor.visitStruct(this, p);
  }

  public @NotNull DefVar<StructDef, Decl.StructDecl> ref() {
    return ref;
  }
}
