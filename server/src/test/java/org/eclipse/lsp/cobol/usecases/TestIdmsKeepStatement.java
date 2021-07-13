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
package org.eclipse.lsp.cobol.usecases;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.eclipse.lsp.cobol.usecases.engine.UseCaseEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/** Test IDMS KEEP DML statements */
class TestIdmsKeepStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "        DATA DIVISION. \r\n"
          + "        WORKING-STORAGE SECTION. \r\n"
          + "        01 {$*WK_ID} PIC X(8).\n"
          + "        01 {$*WK_LOC} PIC S9(8) COMP.\n"
          + "        01 {$*EMPLOYEE} PIC S9(8) COMP.\n"
          + "        01 {$*DEPT-EMP} PIC S9(8) COMP.\n"
          + "        PROCEDURE DIVISION.\n";

  private static final String KEEP1 = DEFS + "           KEEP CURRENT {$EMPLOYEE}.\n";

  private static final String KEEP2 =
      DEFS + "           KEEP EXCLUSIVE CURRENT WITHIN {$DEPT-EMP}\n";

  private static final String KEEP3 = DEFS + "           KEEP CURRENT.\n";

  private static final String KEEP_LONGTERM_RELEASE =
      DEFS + "           KEEP LONGTERM ALL RELEASE.\n";

  private static final String KEEP_LONGTERM_TEST =
      DEFS + "           KEEP LONGTERM 'TSTID' TEST RETURN NOTIFICATION {$WK_LOC}.\n";

  private static final String KEEP_LOCK_UPGRADE =
      DEFS
          + "           MOVE 'TSTID' TO {$WK_ID}.\r\n"
          + "           KEEP LONGTERM {$WK_ID} UPGRADE EXCLUSIVE RETURN NOTIFICATION\r\n"
          + "           INTO {$WK_LOC}.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(
        KEEP1, KEEP2, KEEP3, KEEP_LONGTERM_RELEASE, KEEP_LONGTERM_TEST, KEEP_LOCK_UPGRADE);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms keep tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
