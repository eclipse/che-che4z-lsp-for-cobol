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

/** Test IDMS Find/Obtain DML statements */
class TestIdmsFindObtainStatements {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*EMPDBK} PIC S9(8) COMP.\n"
          + "       01 {$*EMPPGI} PIC S9(8) COMP.\n"
          + "       01 {$*EMPL-ID} PIC X(4).\n"
          + "       01 {$*ERROR-STATUS} PIC X(4) VALUE '1400'.\n"
          + "           88 {$*DB-END-OF-SET} VALUE '0307'.\n"
          + "       01 {$*EMPLOYEE} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*DEPT-EMPLOYEE} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*DEPT-AREA} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*EMP-AREA} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*OFFICE-AREA} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*OFFICE} PIC S9(8) COMP SYNC.\n"
          + "       01 {$*DEPARTMENT} PIC S9(8) COMP SYNC.\n"
          + "        PROCEDURE DIVISION.\n";

  private static final String FIND1 = DEFS + "           FIND CALC {$EMPLOYEE}.\n";

  private static final String FIND2 =
      DEFS + "           FIND KEEP EXCLUSIVE DUPLICATE {$EMPLOYEE}.\n";

  private static final String FIND3 = DEFS + "           FIND KEEP ANY {$EMPLOYEE}.\n";

  private static final String FIND4 = DEFS + "           OBTAIN CURRENT {$DEPARTMENT},\n";

  private static final String FIND5 = DEFS + "           OBTAIN KEEP CURRENT WITHIN {$DEPT-AREA}\n";

  private static final String FIND6 = DEFS + "           FIND DB-KEY IS {$EMPDBK}.\n";

  private static final String FIND7 =
      DEFS + "           FIND DB-KEY IS {$EMPDBK} PAGE-INFO {$EMPPGI}.\n";

  private static final String FIND8 = DEFS + "           FIND {$EMPLOYEE} DB-KEY IS {$EMPDBK}.\n";

  private static final String FIND9 =
      DEFS + "           FIND DB-KEY IS {$EMPDBK} PAGE-INFO {$EMPPGI}.\n";

  private static final String FIND10 = DEFS + "           OBTAIN OWNER WITHIN {$DEPT-EMPLOYEE}\n";

  private static final String FIND11 = DEFS + "           FIND FIRST WITHIN {$EMP-AREA}\n";

  private static final String FIND12 =
      DEFS + "           OBTAIN KEEP EXCLUSIVE PRIOR {$DEPARTMENT} WITHIN {$DEPT-AREA}\n";

  private static final String FIND13 =
      DEFS + "           OBTAIN -10 {$OFFICE} WITHIN {$OFFICE-AREA}.\n";

  private static final String FIND14 = DEFS + "           FIND 10 WITHIN {$OFFICE-AREA}.\n";

  private static final String FIND15 =
      DEFS + "           OBTAIN {$EMPLOYEE} WITHIN {$DEPT-EMPLOYEE} CURRENT USING {$EMPL-ID}.\n";

  private static final String FIND16 =
      DEFS + "           OBTAIN {$EMPLOYEE} WITHIN {$DEPT-EMPLOYEE} USING {$EMPL-ID};\n";

  private static final String FIND17 =
      DEFS + "           FIND KEEP EXCLUSIVE DUPLICATE {$EMPLOYEE}.\n";

  private static final String FIND18 =
      DEFS
          + "           OBTAIN NEXT {$EMPLOYEE} WITHIN {$DEPT-EMPLOYEE} ON {$DB-END-OF-SET}\n"
          + "                   DISPLAY 'END OF SET'.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(
        FIND1, FIND2, FIND3, FIND4, FIND5, FIND6, FIND7, FIND8, FIND9, FIND10, FIND11, FIND12,
        FIND13, FIND14, FIND15, FIND16, FIND17, FIND18);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms find/obtain tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
