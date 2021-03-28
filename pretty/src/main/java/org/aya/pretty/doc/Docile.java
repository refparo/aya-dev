// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
package org.aya.pretty.doc;

import org.jetbrains.annotations.NotNull;

/**
 * Should be called <code>Prettiable</code>
 */
public interface Docile {
  @NotNull Doc toDoc();
}