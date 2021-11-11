// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.api.core;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.DynamicSeq;
import org.aya.api.distill.AyaDocile;
import org.aya.api.ref.LocalVar;
import org.aya.api.ref.Var;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author kiva, ice1000
 */
@ApiStatus.NonExtendable
public interface CoreTerm extends AyaDocile {
  /** @return Number of usages of the given var. */
  int findUsages(@NotNull Var var);
  /**
   * Perform a scope-check for a given term.
   *
   * @param allowed variables allowed in this term.
   * @return the variables in this term that are not allowed.
   */
  @NotNull DynamicSeq<LocalVar> scopeCheck(@NotNull ImmutableSeq<LocalVar> allowed);
  @Nullable CorePat toPat(boolean explicit);
}
