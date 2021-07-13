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

/** Test IDMS Erase DML statements */
class TestIdmsEraseStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "        DATA DIVISION.\n"
          + "        WORKING-STORAGE SECTION.\n"
          + "        01 {$*EMPLOYEE} PIC X(8).\n"
          + "        PROCEDURE DIVISION.\n";

  private static final String ERAS1 = DEFS + "           ERASE {$EMPLOYEE} SELECTIVE MEMBERS\n";

  private static final String ERAS2 = DEFS + "           ERASE {$EMPLOYEE} ALL MEMBERS\n";

  private static final String ERAS3 = DEFS + "           ERASE {$EMPLOYEE} PERMANENT MEMBERS\n";

  private static final String ERAS4 = DEFS + "           ERASE {$EMPLOYEE}.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(ERAS1, ERAS2, ERAS3, ERAS4);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms erase tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
