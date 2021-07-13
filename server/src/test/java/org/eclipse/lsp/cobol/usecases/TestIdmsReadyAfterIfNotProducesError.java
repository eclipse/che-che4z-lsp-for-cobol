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
import org.junit.jupiter.api.Test;

/** This test checks that the IDMS READY statement is parsed correctly after the IF statement */
class TestIdmsReadyAfterIfNotProducesError {
  private static final String TEXT =
      "       identification division.\n"
          + "       program-id. test.\n"
          + "       data division.\n"
          + "       working-storage section.\n"
          + "       01 {$*AN-IDMS-AREA}     USAGE IS POINTER.\n"
          + "       01 {$*SECOND-IDMS-AREA} USAGE IS POINTER.\n"
          + "       01 {$*SOME-IDMS-AREA} USAGE IS POINTER.\n"
          + "       01 {$*SOME-OTHER-AREA} USAGE IS POINTER.\n"
          + "           88 {$*UPDATE-MODE} VALUE TRUE.\n"
          + "       01 {$*WS-EXAMPLE} PIC X(5) VALUE \"HELLO\".\n"
          + "       procedure division.\n"
          + "       {#*0100-MAINLINE}.\n"
          + "           BIND RUN-UNIT.\n"
          + "           READY {$SOME-IDMS-AREA}  USAGE-MODE IS RETRIEVAL\n"
          + "           READY {$SOME-OTHER-AREA} USAGE-MODE IS RETRIEVAL\n"
          + "           if {$UPDATE-MODE} \n"
          + "                READY {$AN-IDMS-AREA}     USAGE-MODE iS UPDATE\n"
          + "                READY {$SECOND-IDMS-AREA} USAGE-MODE iS UPDATE\n"
          + "           ELSE\n"
          + "                READY {$AN-IDMS-AREA}     USAGE-MODE iS RETRIEVAL\n"
          + "                READY {$SECOND-IDMS-AREA} USAGE-MODE iS RETRIEVAL\n"
          + "           END-IF\n"
          + "           DISPLAY {$WS-EXAMPLE}\n"
          + "           .\n"
          + "       {#*0100-EXIT}.\n"
          + "           STOP RUN.";

  @Test
  void test() {
    UseCaseEngine.runTest(TEXT, ImmutableList.of(), ImmutableMap.of());
  }
}
