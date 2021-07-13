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

/** Test IDMS Get DML statements */
class TestIdmsGetStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "        DATA DIVISION.\n"
          + "        WORKING-STORAGE SECTION.\n"
          + "        01 {$*WK_NAME} PIC X(8).\n"
          + "        01 {$*WK_AREA1} PIC X(8).\n"
          + "        01 {$*WK_AREA2} PIC X(8).\n"
          + "        01 {$*WK_LENGTH} PIC S9(8).\n"
          + "        01 {$*WK_ID} PIC S9(8) COMP.\n"
          + "        01 {$*WK_TIME} PIC S9(11) COMP-3.\n"
          + "        01 {$*WK_DATE} PIC S9(7).\n"
          + "        01 {$*EMPLOYEE} PIC S9(7).\n"
          + "        01 {$*EMP-AREA} PIC S9(7).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String GET1 =
      DEFS
          + "           FIND FIRST {$EMPLOYEE} WITHIN {$EMP-AREA}.\n"
          + "           GET {$EMPLOYEE}.\n";

  private static final String GET2 =
      DEFS + "           FIND FIRST {$EMPLOYEE} WITHIN {$EMP-AREA}\n" + "           GET\n";

  private static final String GET_QUEUE =
      DEFS + "           GET QUEUE INTO {$WK_AREA1}\n" + "           TO {$WK_AREA2}.\n";

  private static final String GET_QUEUE_WITH_ALL_LITERALS =
      DEFS
          + "           MOVE '123' TO {$WK_ID}. GET QUEUE ID 'TESTQUE' RECORD\r\n"
          + "           ID {$WK_ID} DELETE LOCK NOWAIT INTO {$WK_AREA1} MAX\r\n"
          + "           LENGTH 8 RETURN LENGTH INTO {$WK_AREA2}.\r\n";

  private static final String GET_SCRATCH =
      DEFS + "           GET SCRATCH INTO {$WK_AREA1} TO {$WK_AREA2}.\n";

  private static final String GET_SCRATCH_WITH_ALL_LITERALS =
      DEFS
          + "           MOVE '123' TO {$WK_ID}. GET SCRATCH AREA ID 'TSTID'\r\n"
          + "           RECORD ID {$WK_ID} KEEP INTO {$WK_AREA1} MAX\r\n"
          + "           LENGTH 8 RETURN LENGTH INTO {$WK_AREA2}.\r\n";

  private static final String GET_SCRATCH_WITH_ALL_VARIABLES =
      DEFS
          + "           MOVE '123' TO {$WK_ID}. MOVE 8 TO {$WK_LENGTH}. GET SCRATCH\r\n"
          + "           AREA ID {$WK_ID} RECORD ID {$WK_ID} INTO {$WK_AREA1} MAX\r\n"
          + "           LENGTH {$WK_LENGTH} RETURN LENGTH INTO {$WK_AREA2}.\r\n";

  private static final String GET_STORAGE = DEFS + "           GET STORAGE FOR {$WK_AREA1}.";

  private static final String GET_STORAGE_WITH_ALL_PARMS =
      DEFS
          + "           MOVE 8 TO {$WK_LENGTH}.\r\n"
          + "           MOVE '123' TO {$WK_ID}. GET STORAGE FOR {$WK_AREA1}\r\n"
          + "           TO {$WK_AREA2} LENGTH {$WK_LENGTH} WAIT KEEP LONG\r\n"
          + "           SHARED STGID '123' VALUE IS HIGH-VALUE LOCATION IS ANY.";

  private static final String GET_TIME = DEFS + "           GET TIME.\r\n";

  private static final String GET_TIME_ALL_PARMS =
      DEFS + "           GET TIME INTO {$WK_TIME} COMP-3 DATE INTO {$WK_DATE}.\r\n";

  private static Stream<String> textsToTest() {
    return Stream.of(
        GET1,
        GET2,
        GET_QUEUE,
        GET_QUEUE_WITH_ALL_LITERALS,
        GET_SCRATCH,
        GET_SCRATCH_WITH_ALL_LITERALS,
        GET_SCRATCH_WITH_ALL_VARIABLES,
        GET_STORAGE,
        GET_STORAGE_WITH_ALL_PARMS,
        GET_TIME,
        GET_TIME_ALL_PARMS);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms get test")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
