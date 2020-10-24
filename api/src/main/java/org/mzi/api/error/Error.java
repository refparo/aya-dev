// Copyright (c) 2020-2020 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the Apache-2.0 license that can be found in the LICENSE file.
package org.mzi.api.error;

import org.jetbrains.annotations.NotNull;

/**
 * @author ice1000
 */
public interface Error {
  enum Level {
    INFO, WARN_UNUSED {
      @Override
      public String toString() {
        return "WARN";
      }
    }, GOAL, WARN, ERROR
  }

  enum Stage {TERCK, TYCK, RESOLVE, PARSE, OTHER}

  @NotNull Level level();
  default @NotNull Stage stage() {
    return Stage.OTHER;
  }
}
