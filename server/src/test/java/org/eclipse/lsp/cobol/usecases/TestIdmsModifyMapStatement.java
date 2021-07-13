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

/** Test IDMS MODIFY MAP statements */
class TestIdmsModifyMapStatement {

  private static final String DEFS =
      "        IDENTIFICATION DIVISION.\n"
          + "        PROGRAM-ID. test1.\n"
          + "       DATA DIVISION.\n"
          + "       WORKING-STORAGE SECTION.\n"
          + "       01 {$*MFLD} PIC X(8).\n"
          + "       01 {$*PADFLD} PIC X.\n"
          + "       01 {$*MAP1}.\n"
          + "          02 {$*MFLD2} PIC X(2).\n"
          + "          02 {$*MFLD3} PIC X(3).\n"
          + "       01 {$*CURROW} PIC S9(4).\n"
          + "       01 {$*CURCOL} PIC S9(4).\n"
          + "       01 {$*EMPMAP} PIC S9(4).\n"
          + "       01 {$*MAP2}.\n"
          + "           03 FILLER OCCURS 10 TIMES INDEXED BY {$*S1}.\n"
          + "           05 {$*MAP-FLD} PIC X(5).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String TST1 = DEFS + "           MODIFY MAP {$EMPMAP}.\n";

  private static final String TST2 = DEFS + "           MODIFY MAP {$EMPMAP} TEMPORARY.\n";

  private static final String TST3 =
      DEFS
          + "           MODIFY MAP {$EMPMAP} CURSOR AT {$CURROW} {$CURCOL}\n"
          + "           WCC RESETMDT 40CR ALARM \n";

  private static final String TST4 =
      DEFS
          + "           MODIFY MAP {$EMPMAP} CURSOR DFLD {$MFLD2}\n"
          + "           FOR ALL EXCEPT CURRENT OUTPUT DATA IS YES INPUT DATA NO.\n";

  private static final String TST5 =
      DEFS
          + "           MODIFY MAP {$EMPMAP} FOR DFLD {$MFLD2} DFLD {$MFLD3}\n"
          + "           BACKSCAN EDIT IS ERROR ERROR MESSAGE SUPPRESS.\n";

  private static final String TST6 =
      DEFS
          + "           MODIFY MAP {$EMPMAP} PERMANENT FOR ALL FIELDS PAD '0' OPTIONAL\n"
          + "           ATTRIBUTES UNPROTECTED SKIP REVERSE-VIDEO BLUE \n";

  private static final String TST7 =
      DEFS
          + "           MODIFY MAP {$EMPMAP} FOR ALL ERROR FIELDS\n"
          + "            LEFT JUSTIFY\n"
          + "            PAD {$PADFLD}\n";

  private static final String TST8 =
      DEFS
          + "           MODIFY MAP {$EMPMAP} \n"
          + "             FOR ALL EXCEPT DFLD {$MFLD2} PAD LOW-VALUE.\n";

  private static final String TST9 =
      DEFS
          + "           MODIFY MAP {$EMPMAP} FOR ALL DFLD {$MAP-FLD}({$S1}) \n"
          + "                             DFLD  {$MFLD2} OF {$MAP1} \n"
          + "                             DFLD  {$MAP-FLD}(3) \n "
          + "                             RIGHT JUSTIFY ATTRIBUTES DETECT DARK.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(TST1, TST2, TST3, TST4, TST5, TST6, TST7, TST8, TST9);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms modify map in tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
