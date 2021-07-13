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

/** Test IDMS MAP IN statements */
class TestIdmsMapInStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*MFLD} PIC X(8).\n"
          + "       01 {$*MAP1}.\n"
          + "          02 {$*MFLD2} PIC X(2).\n"
          + "          02 {$*MFLD3} PIC X(3).\n"
          + "       01 {$*RETKEY} PIC X(4).\n"
          + "       01 {$*SEQ-NUM} PIC S9(8).\n"
          + "       01 {$*PAG-NUM} PIC S9(8).\n"
          + "       01 {$*DETKEY} PIC S9(8) COMP.\n"
          + "       01 {$*MFLD-LEN} PIC S9(4).\n"
          + "       01 {$*EMPMAP} PIC S9(4).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String TST1 =
      DEFS + "                MAP IN USING {$EMPMAP} INPUT DATA IS YES.\n";

  private static final String TST2 =
      DEFS + "           MAP IN USING {$EMPMAP} IO HEADER MODIFIED.\n";

  private static final String TST3 =
      DEFS + "           MAP IN USING {$EMPMAP} NOIO DATASTREAM FROM {$MFLD} LENGTH {$MFLD-LEN}.\n";

  private static final String TST4 =
      DEFS + "           MAP IN USING {$EMPMAP} NOIO DATASTREAM FROM {$MFLD} LENGTH 20.\n";

  private static final String TST5 =
      DEFS
          + "           MAP IN USING {$EMPMAP} NOIO DATASTREAM FROM {$MAP1} TO {$MFLD3}\n"
          + "              DETAIL FIRST RETURNKEY {$RETKEY}.\n";

  private static final String TST6 =
      DEFS
          + "           MAP IN USING {$EMPMAP} DETAIL SEQUENCE NUMBER {$SEQ-NUM}\n"
          + "               PAGE IS {$PAG-NUM}.\n";

  private static final String TST7 =
      DEFS
          + "           MAP IN USING {$EMPMAP}\n"
          + "             INPUT DATA NO DETAIL KEY {$DETKEY}.\n";

  private static final String TST8 =
      DEFS + "           MAP IN USING {$EMPMAP} DETAIL NEXT MODIFIED.\n";

  private static final String TST9 =
      DEFS + "           MAP IN USING {$EMPMAP} HEADER PAGE IS {$PAG-NUM} MODIFIED.\n";

  private static final String TST10 =
      DEFS + "           MAP IN USING {$EMPMAP} IO INPUT DATA YES.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(TST1, TST2, TST3, TST4, TST5, TST6, TST7, TST8, TST9, TST10);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms map in tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
