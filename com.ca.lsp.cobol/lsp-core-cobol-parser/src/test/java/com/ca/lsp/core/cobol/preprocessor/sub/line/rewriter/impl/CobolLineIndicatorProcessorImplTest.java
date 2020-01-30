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
 */
package com.ca.lsp.core.cobol.preprocessor.sub.line.rewriter.impl;

import static com.ca.lsp.core.cobol.preprocessor.CobolSourceFormat.FIXED;
import static com.ca.lsp.core.cobol.preprocessor.ProcessingConstants.COMMENT_TAG;
import static com.ca.lsp.core.cobol.preprocessor.sub.CobolLineTypeEnum.COMMENT;
import static com.ca.lsp.core.cobol.preprocessor.sub.CobolLineTypeEnum.COMPILER_DIRECTIVE;
import static com.ca.lsp.core.cobol.preprocessor.sub.CobolLineTypeEnum.CONTINUATION;
import static com.ca.lsp.core.cobol.preprocessor.sub.CobolLineTypeEnum.NORMAL;
import static com.ca.lsp.core.cobol.preprocessor.sub.CobolLineTypeEnum.DEBUG;
import static com.ca.lsp.core.cobol.preprocessor.ProcessingConstants.WS;
import static org.junit.Assert.assertEquals;

import com.ca.lsp.core.cobol.preprocessor.sub.CobolLine;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class CobolLineIndicatorProcessorImplTest {

  private static final String EMPTY_STRING = "";

  /**
   * Testing preprocessing formatting for COBOL files, making sure that only needed information is
   * being passed to the parser to avoid issues and making token recognition easier
   */
  @Test
  public void debugLineTest() {
    CobolLine debugLine = new CobolLine();
    debugLine.setType(DEBUG);
    debugLine.setIndicatorArea(WS);
    debugLine.setContentAreaA("    ");
    debugLine.setContentAreaB("     DEBUG LINE HERE      ");

    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();
    CobolLine outcome = processor.processLine(debugLine);

    assertEquals(
        WS + "         DEBUG LINE HERE", outcome.getIndicatorArea() + outcome.getContentArea());
  }

  @Test
  public void normalLineTest() {
    CobolLine normalLine = new CobolLine();
    normalLine.setType(NORMAL);
    normalLine.setFormat(FIXED);
    normalLine.setIndicatorArea(WS);
    normalLine.setContentAreaA("    ");
    normalLine.setContentAreaB("         RANDOM TEXT ,  ");

    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();
    CobolLine outcome = processor.processLine(normalLine);

    assertEquals(
        WS + "             RANDOM TEXT , ", outcome.getIndicatorArea() + outcome.getContentArea());
  }

  @Test
  public void compilerDirectiveTest() {
    CobolLine compilerDirectiveLine = new CobolLine();
    compilerDirectiveLine.setType(COMPILER_DIRECTIVE);
    compilerDirectiveLine.setIndicatorArea(WS);
    compilerDirectiveLine.setContentAreaA("    ");
    compilerDirectiveLine.setContentAreaB("DEFINE");

    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();
    CobolLine outcome = processor.processLine(compilerDirectiveLine);

    assertEquals(WS + EMPTY_STRING, outcome.getIndicatorArea() + outcome.getContentArea());
  }

  @Test
  public void commentLineTest() {
    CobolLine commentLine = new CobolLine();
    commentLine.setType(COMMENT);
    commentLine.setIndicatorArea("*");
    commentLine.setContentAreaA("    ");
    commentLine.setContentAreaB("THIS IS A COMMENT        ");

    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();

    CobolLine outcome = processor.processLine(commentLine);

    assertEquals(
        COMMENT_TAG + WS + "    THIS IS A COMMENT",
        outcome.getIndicatorArea() + outcome.getContentArea());
  }

  @Test
  public void continuationLineTest() {
    CobolLine startContinuationLine = new CobolLine();
    startContinuationLine.setType(NORMAL);
    startContinuationLine.setIndicatorArea(WS);
    startContinuationLine.setContentAreaA("    ");
    startContinuationLine.setContentAreaB("       \"RANDOM TEXT   ");

    CobolLine middleContinuationLine = new CobolLine();
    middleContinuationLine.setType(CONTINUATION);
    middleContinuationLine.setIndicatorArea("-");
    middleContinuationLine.setContentAreaA("    ");
    middleContinuationLine.setContentAreaB("        \"RANDOM TEXT   ");

    CobolLine lastContinuationLine = new CobolLine();
    lastContinuationLine.setType(CONTINUATION);
    lastContinuationLine.setIndicatorArea("-");
    lastContinuationLine.setContentAreaA("    ");
    lastContinuationLine.setContentAreaB("        \"CONTINUED LINE ENDS HERE\"     ");

    startContinuationLine.setSuccessor(middleContinuationLine);
    middleContinuationLine.setSuccessor(lastContinuationLine);

    List<CobolLine> listOfLines =
        Arrays.asList(startContinuationLine, middleContinuationLine, lastContinuationLine);
    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();
    List<CobolLine> outcomeList = processor.processLines(listOfLines);

    assertEquals(
        WS + "           \"RANDOM TEXT   ",
        outcomeList.get(0).getIndicatorArea() + outcomeList.get(0).getContentArea());
    assertEquals(
        WS + "RANDOM TEXT   ",
        outcomeList.get(1).getIndicatorArea() + outcomeList.get(1).getContentArea());
    assertEquals(
        WS + "\"CONTINUED LINE ENDS HERE\"",
        outcomeList.get(2).getIndicatorArea() + outcomeList.get(2).getContentArea());
  }

  @Test
  public void emptyContinuationLine() {
    CobolLine continuationLine = new CobolLine();
    continuationLine.setType(NORMAL);
    continuationLine.setIndicatorArea(WS);
    continuationLine.setContentAreaA("    ");
    continuationLine.setContentAreaB("       \"RANDOM TEXT   ");

    CobolLine emptyContinuationLine = new CobolLine();
    emptyContinuationLine.setType(CONTINUATION);
    emptyContinuationLine.setIndicatorArea("-");
    emptyContinuationLine.setContentAreaA("    ");
    emptyContinuationLine.setContentAreaB("           ");

    continuationLine.setSuccessor(emptyContinuationLine);

    final List<CobolLine> listOfLines = Arrays.asList(continuationLine, emptyContinuationLine);
    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();
    List<CobolLine> outcomeList = processor.processLines(listOfLines);

    assertEquals(
        WS + "           \"RANDOM TEXT   ",
        outcomeList.get(0).getIndicatorArea() + outcomeList.get(0).getContentArea());
    assertEquals(
        WS + EMPTY_STRING,
        outcomeList.get(1).getIndicatorArea() + outcomeList.get(1).getContentArea());
  }

  @Test
  public void trailingCommaContinuationLineTest() {
    CobolLine startContinuationLine = new CobolLine();
    startContinuationLine.setType(NORMAL);
    startContinuationLine.setIndicatorArea(WS);
    startContinuationLine.setContentAreaA("       \"RANDOM TEXT   ");
    startContinuationLine.setContentAreaB("        ");

    CobolLine trailingCommaContinuationLine = new CobolLine();
    trailingCommaContinuationLine.setType(CONTINUATION);
    trailingCommaContinuationLine.setIndicatorArea("-");
    trailingCommaContinuationLine.setContentAreaA("    ");
    trailingCommaContinuationLine.setContentAreaB("         ,");

    startContinuationLine.setSuccessor(trailingCommaContinuationLine);
    trailingCommaContinuationLine.setPredecessor(startContinuationLine);

    List<CobolLine> listOfLines =
        Arrays.asList(startContinuationLine, trailingCommaContinuationLine);
    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();
    List<CobolLine> outcomeList = processor.processLines(listOfLines);

    assertEquals(
        WS + "       \"RANDOM TEXT           ",
        outcomeList.get(0).getIndicatorArea() + outcomeList.get(0).getContentArea());
    assertEquals(
        WS + "             ," + WS,
        outcomeList.get(1).getIndicatorArea() + outcomeList.get(1).getContentArea());
  }

  @Test
  public void continuationLineWithoutBeginningQuotes() {
    CobolLine startContinuationLine = new CobolLine();
    startContinuationLine.setType(NORMAL);
    startContinuationLine.setIndicatorArea(WS);
    startContinuationLine.setContentAreaB("       \"RANDOM TEXT   ");

    CobolLine quoteContinuationLine = new CobolLine();
    quoteContinuationLine.setType(CONTINUATION);
    quoteContinuationLine.setIndicatorArea("-");
    quoteContinuationLine.setContentAreaA("    ");
    quoteContinuationLine.setContentAreaB("          \"RANDOM TEXT SINGLE CONTINUATION LINE\"");

    CobolLine lastContinuationLine = new CobolLine();
    lastContinuationLine.setType(CONTINUATION);
    lastContinuationLine.setIndicatorArea("-");
    lastContinuationLine.setContentAreaA("    ");
    lastContinuationLine.setContentAreaB("          RANDOM TEXT SINGLE CONTINUATION LINE\"");

    startContinuationLine.setSuccessor(quoteContinuationLine);
    quoteContinuationLine.setSuccessor(lastContinuationLine);

    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();

    CobolLine outcome = processor.processLine(lastContinuationLine);

    assertEquals(
        WS + "RANDOM TEXT SINGLE CONTINUATION LINE\"",
        outcome.getIndicatorArea() + outcome.getContentArea());
  }

  @Test
  public void continuationLineWithOuterQuotes() {
    CobolLine startContinuationLine = new CobolLine();
    startContinuationLine.setType(NORMAL);
    startContinuationLine.setIndicatorArea(WS);
    startContinuationLine.setContentAreaB("       \"RANDOM TEXT   ");

    CobolLine quoteContinuationLine = new CobolLine();
    quoteContinuationLine.setType(CONTINUATION);
    quoteContinuationLine.setIndicatorArea("-");
    quoteContinuationLine.setContentAreaA("    ");
    quoteContinuationLine.setContentAreaB("          \"RANDOM TEXT SINGLE CONTINUATION LINE\"");

    CobolLine lastContinuationLine = new CobolLine();
    lastContinuationLine.setType(CONTINUATION);
    lastContinuationLine.setIndicatorArea("-");
    lastContinuationLine.setContentAreaA("    ");
    lastContinuationLine.setContentAreaB("          \"RANDOM TEXT SINGLE CONTINUATION LINE\"");

    startContinuationLine.setSuccessor(quoteContinuationLine);
    quoteContinuationLine.setSuccessor(lastContinuationLine);

    CobolLineIndicatorProcessorImpl processor = new CobolLineIndicatorProcessorImpl();

    CobolLine outcome = processor.processLine(lastContinuationLine);

    assertEquals(
        WS + "RANDOM TEXT SINGLE CONTINUATION LINE\"",
        outcome.getIndicatorArea() + outcome.getContentArea());
  }
}
