/*
 * Copyright (c) 2019 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom, Inc. - initial API and implementation
 */
package com.ca.lsp.core.cobol.preprocessor.sub.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class PreprocessorStringUtilsTest {

  final String TEST_SINGLE_QUOTE_STRING = "\'TESTING IF ALL QUOTES WILL BE REMOVED\'";
  final String TEST_DOUBLE_QUOTE_STRING = "\"TESTING IF ALL QUOTES WILL BE REMOVED\"";
  final String EXPECTED_STRING = "TESTING IF ALL QUOTES WILL BE REMOVED";

  @Test
  public void trimQuotesTest() {
    assertEquals(EXPECTED_STRING,PreprocessorStringUtils.trimQuotes(TEST_SINGLE_QUOTE_STRING));
    assertEquals(EXPECTED_STRING,PreprocessorStringUtils.trimQuotes(TEST_DOUBLE_QUOTE_STRING));
  }
}
