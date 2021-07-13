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

/** Test IDMS MAP OUT statements */
class TestIdmsMapOutStatement {

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
          + "       01 {$*MAP2}.\n"
          + "           03 FILLER OCCURS 10 TIMES INDEXED BY {$*S1}.\n"
          + "           05 {$*MAP-FLD} PIC X(5).\n"
          + "       PROCEDURE DIVISION.\n";

  private static final String TST1 = DEFS + "                MAP OUT USING {$EMPMAP}.\n";

  private static final String TST2 =
      DEFS
          + "           MAP OUT USING {$EMPMAP} NOWAIT OUTPUT DATA YES\n"
          + "               RESUME PAGE {$PAG-NUM}.\n";

  private static final String TST3 =
      DEFS + "           MAP OUT USING {$EMPMAP} IO OUTPUT LITERALS DETAIL KEY IS {$DETKEY} .\n";

  private static final String TST4 =
      DEFS + "           MAP OUT USING {$EMPMAP} NOIO DATASTREAM INTO {$MFLD} LENGTH 20.\n";

  private static final String TST5 =
      DEFS
          + "           MAP OUT USING {$EMPMAP} NOIO DATASTREAM INTO {$MAP1} TO {$MFLD3}\n"
          + "              DETAIL NEW.\n";

  private static final String TST6 =
      DEFS
          + "           MAP OUT USING {$EMPMAP} MESSAGE IS {$MFLD} LENGTH {$MFLD-LEN}\n"
          + "               RESUME.\n";

  private static final String TST7 =
      DEFS
          + "           MAP OUT USING {$EMPMAP} NOIO DATASTREAM INTO {$MAP1}\n"
          + "               LENGTH {$MFLD-LEN} RETURN LENGTH INTO {$RETKEY}\n"
          + "               OUTPUT DATA ATTRIBUTE ERASE LITERALS.\n";

  private static final String TST8 = DEFS + "           MAP OUT USING {$EMPMAP} RESUME.\n";

  private static Stream<String> textsToTest() {
    return Stream.of(TST1, TST2, TST3, TST4, TST5, TST6, TST7, TST8);
  }

  @ParameterizedTest
  @MethodSource("textsToTest")
  @DisplayName("Parameterized - idms map out tests")
  void test(String text) {
    UseCaseEngine.runTest(text, ImmutableList.of(), ImmutableMap.of());
  }
}
