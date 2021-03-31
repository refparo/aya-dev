// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
package org.aya.cli;

import org.aya.api.error.CountingReporter;
import org.aya.api.error.Reporter;
import org.aya.api.util.InterruptException;
import org.aya.concrete.parse.AyaParsing;
import org.aya.concrete.parse.AyaProducer;
import org.aya.concrete.pretty.StmtPrettier;
import org.aya.concrete.resolve.module.CachedModuleLoader;
import org.aya.concrete.resolve.module.FileModuleLoader;
import org.aya.concrete.resolve.module.ModuleListLoader;
import org.aya.core.def.Def;
import org.aya.core.def.PrimDef;
import org.aya.pretty.doc.Doc;
import org.aya.tyck.ExprTycker;
import org.aya.tyck.trace.Trace;
import org.glavo.kala.tuple.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record SingleFileCompiler(@NotNull Reporter reporter, @NotNull Path filePath, Trace.@Nullable Builder builder) {
  public int compile(@NotNull CompilerFlags flags) throws IOException {
    var reporter = new CountingReporter(this.reporter);
    var parser = AyaParsing.parser(filePath, reporter);
    try {
      var program = new AyaProducer(reporter).visitProgram(parser.program());
      var choice = flags.distillChoice();
      var pathFileName = filePath.getFileName().toString();
      var dotIndex = pathFileName.indexOf('.');
      var htmlPath = filePath.resolveSibling(pathFileName
        .substring(0, dotIndex > 0 ? dotIndex : pathFileName.length()) + ".html");
      if (choice == CliArgs.DistillChoice.Raw) {
        // [chuigda]: I suggest 80 columns, or we may detect terminal width with some library
        Files.writeString(htmlPath, Doc.vcat(
          StmtPrettier.INSTANCE.visitAll(program, Unit.unit()).stream()).renderToHtml());
      }
      var loader = new ModuleListLoader(flags.modulePaths().map(path ->
        new CachedModuleLoader(new FileModuleLoader(path, reporter, builder))));
      FileModuleLoader.tyckModule(loader, program, reporter,
        () -> {
          if (choice == CliArgs.DistillChoice.Scoped)
            Files.writeString(htmlPath, Doc.vcat(
              StmtPrettier.INSTANCE.visitAll(program, Unit.unit()).stream()).renderToHtml());
        },
        defs -> {
          if (choice == CliArgs.DistillChoice.Typed)
            Files.writeString(htmlPath, Doc.vcat(defs.map(Def::toDoc)).renderToHtml());
        }, builder);
      PrimDef.clearConcrete();
    } catch (ExprTycker.TyckerException e) {
      FileModuleLoader.handleInternalError(e);
      return e.exitCode();
    } catch (InterruptException e) {
      // TODO[ice]: proper error handling
      reporter.reportString(e.stage().name() + " interrupted due to errors.");
      if (flags.interruptedTrace()) e.printStackTrace();
    }
    if (reporter.isEmpty()) {
      reporter.reportString(flags.message().successNotion());
      return 0;
    } else {
      reporter.reportString(flags.message().failNotion());
      return -1;
    }
  }
}
