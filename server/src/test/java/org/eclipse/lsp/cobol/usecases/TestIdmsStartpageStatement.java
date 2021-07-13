/*
 * Copyright (c) 2021 Broadcom.
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
package org.eclipse.lsp.cobol.usecases;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.eclipse.lsp.cobol.usecases.engine.UseCaseEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/** Test IDMS STARTPAGE statements */
class TestIdmsStartpageStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "        DATA DIVISION.\n"
          + "        WORKING-STORAGE SECTION.\n"
          + "        01 {$*EMPMAP} PIC X(8).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String TST1 = DEFS + "                STARTPAGE {$EMPMAP}.\n";

  private static final String TST2 =
      DEFS + "           STARTPAGE SESSION {$EMPMAP} WAIT NOBACKPAGE BROWSE AUTODISPLAY.\n";

  private static final String TST3 = DEFS + "           STARTPAGE SESSION {$EMPMAP} BACKPAGE.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(TST1, TST2, TST3);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms startpage tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
