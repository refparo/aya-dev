// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
package org.aya.core.sort;

import org.aya.api.ref.LevelVar;
import org.aya.core.sort.Sort.Level;
import org.glavo.kala.collection.mutable.MutableMap;
import org.glavo.kala.collection.mutable.MutableTreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * References in Arend:
 * <a href="https://github.com/JetBrains/Arend/blob/master/base/src/main/java/org/arend/core/subst/StdLevelSubstitution.java"
 * >StdLevelSubstitution.java</a> (as {@link Sort}),
 * <a href="https://github.com/JetBrains/Arend/blob/master/base/src/main/java/org/arend/core/subst/SimpleLevelSubstitution.java"
 * >SimpleLevelSubstitution.java</a>, etc.
 */
public interface LevelSubst {
  boolean isEmpty();
  @Nullable Level get(@NotNull LevelVar<Level> var);
  @NotNull LevelSubst subst(@NotNull LevelSubst subst);
  @NotNull LevelSubst EMPTY = new Simple(MutableTreeMap.of((o1, o2) -> {
    throw new UnsupportedOperationException("Shall not modify LevelSubst.EMPTY");
  }));

  record Simple(@NotNull MutableMap<@NotNull LevelVar<Level>, @NotNull Level> map) implements LevelSubst {
    public void add(@NotNull LevelVar<Level> var, @NotNull Level level) {
      map.put(var, level);
    }

    @Override public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override public @Nullable Level get(@NotNull LevelVar<Level> var) {
      return map.getOrNull(var);
    }

    @Override public @NotNull LevelSubst subst(@NotNull LevelSubst subst) {
      if (isEmpty()) return this;
      map.replaceAll((var, term) -> term.subst(subst));
      return this;
    }
  }
}