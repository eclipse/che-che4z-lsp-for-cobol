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

/** Test IDMS Bind DML statements */
class TestIdmsBindStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*DB1} PIC X(8).\n"
          + "       01 {$*DC1} PIC X(8).\n"
          + "       01 {$*EMPINFO} PIC X(8).\n"
          + "       01 {$*EMPLOYEE} PIC X(8).\n"
          + "       01 {$*ABCPROC} PIC X(8).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String BIND1 =
      DEFS + "           BIND RUN-UNIT DBNODE 'DB1' DICTNAME 'DC1';\n";

  private static final String BIND2 =
      DEFS + "           BIND RUN-UNIT DBNODE {$DB1} DICTNAME {$DC1}.\n";

  private static final String BIND3 = DEFS + "           BIND RUN-UNIT\n";

  private static final String BIND4 = DEFS + "           BIND RUN-UNIT DICTNAME {$DC1}.\n";

  private static final String BIND5 = DEFS + "           BIND MAP {$EMPINFO}.\n";

  private static final String BIND6 = DEFS + "           BIND MAP {$EMPINFO} RECORD {$EMPLOYEE}.\n";

  private static final String BIND7 =
      DEFS + "           BIND MAP {$EMPINFO} RECORD {$EMPLOYEE} TO NULL.\n";

  private static final String BIND8 =
      DEFS + "           BIND MAP {$EMPINFO} RECORD {$EMPLOYEE} TO {$DB1}.\n";

  private static final String BIND9 =
      DEFS + "           BIND PROCEDURE FOR {$ABCPROC} TO {$DB1}.\n";

  private static final String BIND10 = DEFS + "          BIND {$EMPLOYEE}\n";

  private static final String BIND11 = DEFS + "          BIND {$EMPLOYEE} TO {$DB1}.\n";

  private static final String BIND12 = DEFS + "          BIND {$DB1} WITH {$EMPLOYEE}\n";

  private static Stream<String> textsToTest() {
    return Stream.of(
        BIND1, BIND2, BIND3, BIND4, BIND5, BIND6, BIND7, BIND8, BIND9, BIND10, BIND11, BIND12);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms bind tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
