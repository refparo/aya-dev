// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.single;

import org.aya.api.distill.DistillerOptions;
import org.aya.api.error.Problem;
import org.aya.api.error.Reporter;
import org.jetbrains.annotations.NotNull;

/**
 * @author ice1000
 */
public final class CliReporter implements Reporter {
  public static final CliReporter INSTANCE = new CliReporter();

  private CliReporter() {
  }

  @Override public void report(@NotNull Problem problem) {
    var errorMsg = problem.computeFullErrorMessage(DistillerOptions.DEFAULT);
    var level = problem.level();
    if (problem.isError() || level == Problem.Severity.WARN) System.err.println(errorMsg);
    else System.out.println(errorMsg);
  }
}
