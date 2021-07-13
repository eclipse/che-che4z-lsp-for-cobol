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

/** Test IDMS Connect/Disconnect DML statements */
class TestIdmsConDisconnectStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*ERROR-STATUS} PIC X(4) VALUE '1400'.\n"
          + "           88 {$*DB-REC-NOT-FOUND} VALUE '0326'.\n"
          + "       01 {$*EMPLOYEE} PIC X(8).\n"
          + "       01 {$*OFFICE-EMPLOYEE} PIC X(8).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String DISC1 =
      DEFS + "           DISCONNECT {$EMPLOYEE} FROM {$OFFICE-EMPLOYEE}\n";

  private static final String DISC2 =
      DEFS
          + "           DISCONNECT {$EMPLOYEE} FROM {$OFFICE-EMPLOYEE} \n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.  \n";

  private static final String CONN1 =
      DEFS + "           CONNECT {$EMPLOYEE} TO {$OFFICE-EMPLOYEE}\n";

  private static Stream<String> textsToTest() {
    return Stream.of(DISC1, DISC2, CONN1);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms connect and disconnect tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
