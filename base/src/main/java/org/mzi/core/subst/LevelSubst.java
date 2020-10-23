package org.mzi.core.subst;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mzi.api.ref.Var;
import org.mzi.tyck.sort.Sort;
import org.mzi.tyck.sort.Sort.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * References in Arend:
 * <a href="https://github.com/JetBrains/Arend/blob/master/base/src/main/java/org/arend/core/subst/StdLevelSubstitution.java"
 * >StdLevelSubstitution.java</a> (as {@link Sort}),
 * <a href="https://github.com/JetBrains/Arend/blob/master/base/src/main/java/org/arend/core/subst/SimpleLevelSubstitution.java"
 * >SimpleLevelSubstitution.java</a>, etc.
 */
public interface LevelSubst {
  boolean isEmpty();
  @Nullable Level get(@NotNull Var var);
  @NotNull LevelSubst subst(@NotNull LevelSubst subst);
  @NotNull LevelSubst EMPTY = new Simple(Collections.emptyMap());

  class Simple implements LevelSubst {
    private final @NotNull Map<@NotNull Var, @NotNull Level> map;

    public Simple(@NotNull Map<@NotNull Var, @NotNull Level> map) {
      this.map = map;
    }

    public void add(@NotNull Var var, @NotNull Level level) {
      map.put(var, level);
    }

    @Override public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override public @Nullable Level get(@NotNull Var var) {
      return map.get(var);
    }

    @Override public @NotNull LevelSubst subst(@NotNull LevelSubst subst) {
      if (isEmpty()) return this;
      var result = new Simple(new HashMap<>());
      for (var entry : map.entrySet()) result.map.put(entry.getKey(), entry.getValue().subst(subst));
      return result;
    }
  }
}