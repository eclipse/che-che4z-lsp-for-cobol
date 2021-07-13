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

/** Test IDMS ACCEPT DML statements */
class TestIdmsAcceptDbStatements {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*ERROR-STATUS} PIC X(4) VALUE '1400'.\n"
          + "           88 {$*DB-STATUS-OK} VALUE '0000'.\n"
          + "           88 {$*ANY-STATUS} VALUE ' ' THRU '9999'.\n"
          + "           88 {$*ANY-ERROR-STATUS} VALUE '0001' THRU '9999'.\n"
          + "           88 {$*DB-END-OF-SET} VALUE '0307'.\n"
          + "           88 {$*DB-REC-NOT-FOUND} VALUE '0326'.\n"
          + "       01 {$*DB1} PIC X(8).\n"
          + "       01 {$*DC1} PIC X(8).\n"
          + "       01 {$*EMP-LOC} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*DB-KEY1} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*employee} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*DEPT-EMPL} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*EMP_AREA} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*ABC-PROC} PIC S9(8) COMP SYNC.\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String TST1 = DEFS + "           ACCEPT {$DB1} FOR {$EMPLOYEE}.\n";

  private static final String TST2 =
      DEFS + "           ACCEPT {$DB1} FROM {$abc-proc} PROCEDURE.\n";

  private static final String TST3 = DEFS + "           ACCEPT {$EMP-LOC} FROM {$EMPLOYEE} BIND.\n";

  private static final String TST4 = DEFS + "           ACCEPT {$DC1} FROM IDMS-STATISTICS.\n";

  private static final String TST5 =
      DEFS + "           ACCEPT {$DC1} FROM IDMS-STATISTICS EXTENDED {$DB1}.\n";

  private static final String TST6 =
      DEFS + "           ACCEPT {$DB-KEY1} FROM {$DEPT-EMPL} CURRENCY.\n";

  private static final String TST7 = DEFS + "           ACCEPT {$DB-KEY1} FROM CURRENCY.\n";

  private static final String TST8 =
      DEFS + "           ACCEPT {$DB-KEY1} FROM {$DEPT-EMPL} OWNER CURRENCY.\n";

  private static final String TST9 =
      DEFS + "           ACCEPT {$DB-KEY1} FROM {$EMP_AREA} NEXT CURRENCY PAGE-INFO {$DB1}. \n";

  private static final String TST10 =
      DEFS
          + "           ACCEPT {$DB1} FOR {$EMPLOYEE} \n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.\n";

  private static final String TST20 =
      DEFS
          + "           ACCEPT {$DB1} FROM {$ABC-PROC} PROCEDURE\n"
          + "           ON {$ANY-ERROR-STATUS} DISPLAY 'PROC NOT FOUND'.\n";

  private static final String TST30 =
      DEFS
          + "           ACCEPT {$EMP-LOC} FROM {$EMPLOYEE} BIND\n"
          + "           ON {$ANY-STATUS} DISPLAY 'ACCEPT ADDRESS '.\n";

  private static final String TST40 =
      DEFS
          + "           ACCEPT {$DC1} FROM IDMS-STATISTICS \n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.\n";

  private static final String TST50 =
      DEFS
          + "           ACCEPT {$DC1} FROM IDMS-STATISTICS EXTENDED {$DB1}\n"
          + "           ON {$ANY-ERROR-STATUS} DISPLAY 'ERROR'.\n";

  private static final String TST60 =
      DEFS
          + "           ACCEPT {$DB-KEY1} FROM {$DEPT-EMPL} CURRENCY\n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.\n";

  private static final String TST70 =
      DEFS
          + "           ACCEPT {$DB-KEY1} FROM CURRENCY\n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.\n";
  private static final String TST80 =
      DEFS
          + "           ACCEPT {$DB-KEY1} FROM {$DEPT-EMPL} OWNER CURRENCY\n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.\n";
  private static final String TST90 =
      DEFS
          + "           ACCEPT {$DB-KEY1} FROM {$EMP_AREA} NEXT CURRENCY PAGE-INFO {$DB1} \n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(
        TST1, TST2, TST3, TST4, TST5, TST6, TST7, TST8, TST9, TST10, TST20, TST30, TST40, TST50,
        TST60, TST70, TST80, TST90);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms accept tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
