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

/** Test IDMS READY DML statements */
class TestIdmsReadyStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*DB1} PIC X(8).\n"
          + "       01 {$*DC1} PIC X(8).\n"
          + "       01 {$*EMP-AREA} PIC X(8).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String READ1 = DEFS + "          BIND RUN-UNIT.\n" + "          READY.\n";

  private static final String READ2 =
      DEFS + "          BIND RUN-UNIT.\n" + "          READY {$EMP-AREA}.\n";

  private static final String READ3 =
      DEFS + "          BIND RUN-UNIT.\n" + "          READY {$EMP-AREA} USAGE-MODE UPDATE\n";

  private static final String READ4 =
      DEFS + "          BIND RUN-UNIT.\n" + "          READY {$EMP-AREA} USAGE-MODE RETRIEVAL.\n";

  private static final String READ5 =
      DEFS
          + "          BIND RUN-UNIT.\n"
          + "          READY {$EMP-AREA} USAGE-MODE PROTECTED RETRIEVAL.\n";

  private static final String READ6 =
      DEFS
          + "          BIND RUN-UNIT.\n"
          + "          READY {$EMP-AREA} USAGE-MODE EXCLUSIVE UPDATE\n";

  private static Stream<String> textsToTest() {
    return Stream.of(READ1, READ2, READ3, READ4, READ5, READ6);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms ready tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
