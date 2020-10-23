package org.mzi.core.term;

import asia.kala.control.Option;
import asia.kala.ref.OptionRef;
import asia.kala.ref.Ref;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mzi.api.ref.Var;
import org.mzi.util.Decision;

/**
 * @author ice1000
 */
public record HoleTerm(
  @NotNull OptionRef<@NotNull Term> solution,
  @NotNull Var var
) implements Term {
  public HoleTerm(@Nullable Term solution, @NotNull Var var) {
    this(new OptionRef<>(Option.of(solution)), var);
  }

  @Override public <P, R> R accept(@NotNull Visitor<P, R> visitor, P p) {
    return visitor.visitHole(this, p);
  }

  @Contract(pure = true) @Override public @NotNull Decision whnf() {
    return Decision.MAYBE;
  }
}