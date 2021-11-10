// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.repl;

import kala.collection.Seq;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.DynamicSeq;
import kala.control.Either;
import kala.value.Ref;
import org.aya.api.error.CountingReporter;
import org.aya.api.error.Reporter;
import org.aya.api.error.SourceFileLocator;
import org.aya.api.util.InterruptException;
import org.aya.api.util.NormalizeMode;
import org.aya.cli.single.CompilerFlags;
import org.aya.cli.single.SingleFileCompiler;
import org.aya.concrete.parse.AyaParsing;
import org.aya.concrete.resolve.context.EmptyContext;
import org.aya.concrete.resolve.module.CachedModuleLoader;
import org.aya.concrete.resolve.module.FileModuleLoader;
import org.aya.concrete.resolve.module.ModuleListLoader;
import org.aya.core.def.Def;
import org.aya.core.term.Term;
import org.aya.tyck.TyckState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public class ReplCompiler {
  final @NotNull CountingReporter reporter;
  private final @Nullable SourceFileLocator locator;
  private final @NotNull ReplContext context;
  private final @NotNull DynamicSeq<Path> modulePaths;

  ReplCompiler(@NotNull Reporter reporter, @Nullable SourceFileLocator locator) {
    this.reporter = new CountingReporter(reporter);
    this.locator = locator;
    this.modulePaths = DynamicSeq.create();
    this.context = new ReplContext(new EmptyContext(this.reporter), ImmutableSeq.of("REPL"));
  }

  /** @see ReplCompiler#compileExpr(String, NormalizeMode) */
  public int loadToContext(@NotNull Path file) throws IOException {
    return new SingleFileCompiler(reporter, null, null)
      .compile(file, r -> context, new CompilerFlags(CompilerFlags.Message.EMOJI, false, null,
        modulePaths.view()), null);
  }

  /**
   * Copied and adapted.
   *
   * @param text the text of code to compile, witch might either be a `program` or an `expr`.
   * @see org.aya.cli.single.SingleFileCompiler#compile
   */
  public @NotNull Either<ImmutableSeq<Def>, Term> compileToContext(@NotNull String text, @NotNull NormalizeMode normalizeMode) {
    if (text.isBlank()) return Either.left(ImmutableSeq.empty());
    var locator = this.locator != null ? this.locator : new SourceFileLocator.Module(modulePaths);
    var programOrExpr = AyaParsing.repl(reporter, text);
    try {
      var loader = new ModuleListLoader(modulePaths.view().map(path ->
        new CachedModuleLoader(new FileModuleLoader(locator, path, reporter, null, null))).toImmutableSeq());
      return programOrExpr.map(
        program -> {
          var newDefs = new Ref<ImmutableSeq<Def>>();
          FileModuleLoader.tyckModule(context, loader, program, reporter,
            resolveInfo -> {}, newDefs::set, null);
          var defs = newDefs.get();
          if (reporter.noError()) return defs;
          else {
            // When there are errors, we need to remove the defs from the context.
            var toRemoveDef = DynamicSeq.<String>create();
            context.definitions.forEach((name, mod) -> {
              var toRemoveMod = DynamicSeq.<Seq<String>>create();
              mod.forEach((modName, def) -> {
                if (defs.anyMatch(realDef -> realDef.ref() == def)) toRemoveMod.append(modName);
              });
              if (toRemoveMod.sizeEquals(mod.size())) toRemoveDef.append(name);
              else toRemoveMod.forEach(mod::remove);
            });
            toRemoveDef.forEach(context.definitions::remove);
            return ImmutableSeq.empty();
          }
        },
        expr -> FileModuleLoader.tyckExpr(context, expr, reporter, null).wellTyped()
          .normalize(TyckState.EMPTY, normalizeMode)
      );
    } catch (InterruptException ignored) {
      return Either.left(ImmutableSeq.empty());
    }
  }

  /**
   * Adapted.
   *
   * @see #loadToContext
   */
  public @Nullable Term compileExpr(@NotNull String text, @NotNull NormalizeMode normalizeMode) {
    try {
      var expr = AyaParsing.expr(reporter, text);
      return FileModuleLoader.tyckExpr(context, expr, reporter, null)
        .type().normalize(TyckState.EMPTY, normalizeMode);
    } catch (InterruptException ignored) {
      return null;
    }
  }

  public @NotNull ReplContext getContext() {
    return context;
  }
}
