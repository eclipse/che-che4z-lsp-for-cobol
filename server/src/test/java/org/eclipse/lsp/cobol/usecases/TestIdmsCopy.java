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
import org.eclipse.lsp.cobol.positive.CobolText;
import org.eclipse.lsp.cobol.usecases.engine.UseCaseEngine;
import org.junit.jupiter.api.Test;

/**
 * These test for COPY IDMS statements in FILE SECTION, WS/LINKAGE SECTIONS AND PROCEDURE DIVISION
 */
class TestIdmsCopy {

  private static final String TEXT =
      "        IDENTIFICATION DIVISION. \n"
          + "        PROGRAM-ID. test1.\n"
          + "        DATA DIVISION.\n"
          + "        WORKING-STORAGE SECTION.\n"
          + "        01 {$*EMPLOYEE} PIC X(10).\n";

  private static final String COPY_IDMS_FILE =
      "        FILE SECTION.\n" + "            COPY IDMS FILE {~FL002} VERSION 1.\n";

  private static final String COPY_IDMS_WS1 =
      "       WORKING-STORAGE SECTION.\n"
          + "             01 COPY IDMS {~EMPLOYEE} VERSION 03.\n"
          + "       PROCEDURE DIVISION.\n"
          + "           MOVE 'MYNAME' TO {$EMPNAME}.";

  private static final String COPY_IDMS_WS2 =
      "        WORKING-STORAGE SECTION.\n"
          + "             COPY IDMS {~SUBSCHEMA-RECORDS}\n"
          + "       PROCEDURE DIVISION.\n"
          + "           MOVE 'JAVA' TO {$SKILL-NAME-0455}.";

  private static final String COPY_IDMS_WS3 =
      "        WORKING-STORAGE SECTION.\n"
          + "             COPY IDMS MAP {~ABCMAP}.\n"
          + "       PROCEDURE DIVISION.\n"
          + "           MOVE 'ABC' TO {$MRB-ABCMAP-ID}.\n";

  private static final String COPY_IDMS_WS4 =
      "        WORKING-STORAGE SECTION.\n"
          + "             COPY IDMS {~MAPS}. \n"
          + "       PROCEDURE DIVISION.\n"
          + "           MOVE 'ABC' TO {$MRB-ABCMAP-ID}.\n";

  private static final String COPY_IDMS_WS5 =
      "        WORKING-STORAGE SECTION.\n"
          + "             COPY IDMS MAP-CONTROL {~ABCMAP}.\n"
          + "       PROCEDURE DIVISION.\n"
          + "           MOVE 'ABC' TO {$MRB-ABCMAP-ID}.\n";

  private static final String COPY_IDMS_PD1 =
      "        PROCEDURE DIVISION.\n" + "           COPY IDMS {~SUBSCHEMA-BINDS}\n";

  private static final String COPY_IDMS_PD2 =
      "        PROCEDURE DIVISION.\n" + "           COPY IDMS {~ABCD} VERSION 103.\n";

  private static final String COPY_IDMS_PD3 =
      "        PROCEDURE DIVISION.\n" + "           COPY IDMS MODULE {~ABCD};\n";

  private static final String CB_NAME1 = "EMPLOYEE";
  private static final String CB1 = "          01 {$*EMPNAME}    PIC X(8).\n";

  private static final String CB_NAME2 = "SUBSCHEMA-RECORDS";
  private static final String CB2 =
      "       01  {$*SKILL}.\n"
          + "         02  {$*SKILL-ID-0455}           PIC 9(4).\n"
          + "         02  {$*SKILL-NAME-0455}         PIC X(12).\n"
          + "         02  {$*SKILL-DESCRIPTION-0455}  PIC X(60).\n";

  private static final String CB_NAME3 = "ABCMAP";
  private static final String CB_NAME3A = "MAPS";
  private static final String CB3 =
      "       01  {$*MRB-ABCMAP}.\n" + "            03  {$*MRB-ABCMAP-ID}          PIC X(8).\n";

  private static final String CB_NAME4 = "SUBSCHEMA-BINDS";
  private static final String CB_NAME4A = "ABCD";
  private static final String CB4 =
      "           BIND RUN-UNIT.\n" + "            BIND {$EMPLOYEE}.\n";

  private static final String CB_NAME5 = "FL002";
  private static final String CB5 = "       FD EMP-FILE.\n" + "       RECORD CONTAINS 80.\n";

  @Test
  void testIdmsCopyWS1() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_WS1, ImmutableList.of(new CobolText(CB_NAME1, CB1)), ImmutableMap.of());
  }

  @Test
  void testIdmsCopyWS2() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_WS2, ImmutableList.of(new CobolText(CB_NAME2, CB2)), ImmutableMap.of());
  }

  @Test
  void testIdmsCopyWS3() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_WS3, ImmutableList.of(new CobolText(CB_NAME3, CB3)), ImmutableMap.of());
  }

  @Test
  void testIdmsCopyWS4() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_WS4, ImmutableList.of(new CobolText(CB_NAME3A, CB3)), ImmutableMap.of());
  }

  @Test
  void testIdmsCopyWS5() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_WS5, ImmutableList.of(new CobolText(CB_NAME3, CB3)), ImmutableMap.of());
  }

  @Test
  void testIdmsCopyPD1() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_PD1, ImmutableList.of(new CobolText(CB_NAME4, CB4)), ImmutableMap.of());
  }

  @Test
  void testIdmsCopyPD2() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_PD2, ImmutableList.of(new CobolText(CB_NAME4A, CB4)), ImmutableMap.of());
  }

  @Test
  void testIdmsCopyPD3() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_PD3, ImmutableList.of(new CobolText(CB_NAME4A, CB4)), ImmutableMap.of());
  }

  @Test
  void testIdmsCopyFile() {
    UseCaseEngine.runTest(
        TEXT + COPY_IDMS_FILE, ImmutableList.of(new CobolText(CB_NAME5, CB5)), ImmutableMap.of());
  }
}
