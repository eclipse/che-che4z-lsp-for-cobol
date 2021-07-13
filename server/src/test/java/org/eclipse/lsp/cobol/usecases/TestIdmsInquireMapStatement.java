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

/** Test IDMS INQUIRE MAP statements */
class TestIdmsInquireMapStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*MFLD} PIC X(8).\n"
          + "       01 {$*MAP1}.\n"
          + "          02 {$*MFLD2} PIC X(2).\n"
          + "          02 {$*MFLD3} PIC X(3).\n"
          + "       01 {$*ATT-KEY} PIC X.\n"
          + "       01 {$*CURROW} PIC S9(4).\n"
          + "       01 {$*CURCOL} PIC S9(4).\n"
          + "       01 {$*MFLD-LEN} PIC S9(4).\n"
          + "       01 {$*EMPMAP} PIC S9(4).\n"
          + "       01 {$*MAP2}.\n"
          + "           03 FILLER OCCURS 10 TIMES INDEXED BY {$*S1}.\n"
          + "           05 {$*MAP-FLD} PIC X(5).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String TST1 =
      DEFS + "                INQUIRE MAP {$EMPMAP} MOVE AID TO {$MFLD}.\n";

  private static final String TST2 =
      DEFS + "           INQUIRE MAP {$EMPMAP} MOVE CURSOR TO {$CURROW} {$CURCOL}.\n";

  private static final String TST3 =
      DEFS + "           INQUIRE MAP {$EMPMAP} MOVE IN LENGTH FOR {$MFLD} TO {$MFLD-LEN}.\n";

  private static final String TST4 =
      DEFS + "           INQUIRE MAP {$EMPMAP} IF INPUT CHANGED THEN DISPLAY 'CHANGED'.\n";

  private static final String TST5 =
      DEFS
          + "           INQUIRE MAP {$EMPMAP}\n"
          + "           IF CURSOR AT DFLD {$MFLD2} OF {$MAP1} THEN DISPLAY 'CURSOR'.\n";

  private static final String TST6 =
      DEFS
          + "           INQUIRE MAP {$EMPMAP}\n"
          + "             IF ALL BUT CURRENT DATA YES THEN DISPLAY 'YES'\n"
          + "                                              MOVE 'Y' TO {$ATT-KEY}\n"
          + "             ELSE DISPLAY 'NO'.\n";

  private static final String TST7 =
      DEFS
          + "           INQUIRE MAP {$EMPMAP} IF ALL DFLD {$MAP-FLD}({$S1}) \n"
          + "                             DFLD  {$MFLD2} OF {$MAP1} \n"
          + "                             DFLD  {$MAP-FLD}(3) \n "
          + "                             EDIT IS ERROR THEN DISPLAY 'MAP ERROR'.\n";
  private static final String TST8 =
      DEFS
          + "           INQUIRE MAP {$EMPMAP} IF DFLD {$MAP-FLD}({$S1}) \n"
          + "                             DFLD  {$MFLD2} OF {$MAP1} \n "
          + "                             DFLD  {$MAP-FLD}(3) \n "
          + "                             DATA TRUNCATED THEN DISPLAY 'TRUNCATION'.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(TST1, TST2, TST3, TST4, TST5, TST6, TST7, TST8);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms inquire map tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
