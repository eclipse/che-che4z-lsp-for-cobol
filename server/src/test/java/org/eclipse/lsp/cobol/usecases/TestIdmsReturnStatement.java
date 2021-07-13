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

/** Test IDMS RETURN DML statements */
class TestIdmsReturnStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*EMPDBK} PIC S9(8) COMP.\n"
          + "       01 {$*EMPPGI} PIC S9(8) COMP.\n"
          + "       01 {$*IX-EMP} PIC S9(8) COMP.\n"
          + "       01 {$*INDEX-KEY}.\n"
          + "            03  {$*EMP-LAST}    PIC X(15) VALUE SPACES.\n"
          + "            03  {$*EMP-FIRST}   PIC X(10) VALUE SPACES.\n"
          + "       01 {$*EMPL-KEY} PIC X(25).\n"
          + "       01 {$*ERROR-STATUS} PIC X(4) VALUE '1400'.\n"
          + "           88 {$*DB-REC-NOT-FOUND} VALUE '0326'.\n"
          + "        PROCEDURE DIVISION.\n";

  private static final String TST1 =
      DEFS + "           RETURN {$EMPDBK} FROM {$IX-EMP} CURRENCY.\n";

  private static final String TST2 =
      DEFS + "           RETURN {$EMPDBK} FROM {$IX-EMP} USING {$INDEX-KEY}.\n";

  private static final String TST3 =
      DEFS + "           RETURN {$EMPDBK} FROM {$IX-EMP} PRIOR KEY INTO {$INDEX-KEY}.\n";

  private static final String TST4 =
      DEFS + "           RETURN {$EMPDBK} FROM {$IX-EMP} NEXT CURRENCY KEY {$INDEX-KEY}.\n";

  private static final String TST5 =
      DEFS
          + "           RETURN {$EMPDBK} FROM {$IX-EMP} USING {$INDEX-KEY} KEY INTO {$EMPL-KEY}.\n";

  private static final String TST6 =
      DEFS
          + "           RETURN {$EMPDBK} FROM {$IX-EMP} USING {$INDEX-KEY}\n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.\n";

  private static final String TST7 =
      DEFS
          + "           RETURN {$EMPDBK} FROM {$IX-EMP} USING {$INDEX-KEY} KEY INTO {$EMPL-KEY}\n"
          + "           ON {$DB-REC-NOT-FOUND} DISPLAY 'NOT FOUND'.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(TST1, TST2, TST3, TST4, TST5, TST6, TST7);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms return tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
