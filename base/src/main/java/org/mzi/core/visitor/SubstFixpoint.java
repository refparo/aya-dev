// Copyright (c) 2020-2020 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the Apache-2.0 license that can be found in the LICENSE file.
package org.mzi.core.visitor;

import asia.kala.Unit;
import org.jetbrains.annotations.NotNull;
import org.mzi.tyck.sort.Sort;
import org.mzi.core.subst.LevelSubst;
import org.mzi.core.subst.TermSubst;
import org.mzi.core.term.RefTerm;
import org.mzi.core.term.Term;

/**
 * This doesn't substitute references underlying function calls.
 * @author ice1000
 */
public class SubstFixpoint implements TermFixpoint<Unit> {
  private final @NotNull TermSubst termSubst;
  private final @NotNull LevelSubst levelSubst;

  public SubstFixpoint(@NotNull TermSubst termSubst, @NotNull LevelSubst levelSubst) {
    this.termSubst = termSubst;
    this.levelSubst = levelSubst;
  }

  @Override
  public @NotNull Sort visitSort(@NotNull Sort sort, Unit unused) {
    return sort.substSort(levelSubst);
  }

  @Override
  public @NotNull Term visitRef(@NotNull RefTerm term, Unit unused) {
    return termSubst.get(term.var(), term);
  }
}
