/*
 * Copyright (c) 2020 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Broadcom, Inc. - initial API and implementation
 *
 */

package com.broadcom.lsp.cobol.core.model;

import lombok.Value;

/**
 * This value class represents a resolved copybook. It consists of the copybook name, its URI using
 * for positions and the content.
 */
@Value
public class CopybookModel {
  private String name;
  private String uri;
  private String content;

  /**
   * Create an empty {@link CopybookModel} that contains only the name if the content could not be
   * resolved
   *
   * @param name - a copybook name
   * @return an empty {@link CopybookModel} instance
   */
  public static CopybookModel empty(String name) {
    return new CopybookModel(name, null, null);
  }
}
