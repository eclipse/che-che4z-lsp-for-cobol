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

package org.eclipse.lsp.cobol.core.visitor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.eclipse.lsp.cobol.core.*;
import org.eclipse.lsp.cobol.core.model.EmbeddedCode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.eclipse.lsp.cobol.core.CobolParser.*;

/**
 * This lexer separates embedded language code parts from the main document and parse them with
 * specific parsers.
 */
@AllArgsConstructor
public class EmbeddedLanguagesListener extends CobolParserBaseListener {

  private final DefaultErrorStrategy errorStrategy;
  private final ParseTreeListener treeListener;
  private final ParserListener errorListener;
  @Getter private final Map<Token, EmbeddedCode> embeddedCodeParts = new HashMap<>();

  @Override
  public void exitExecSqlStatementInProcedureDivision(
      ExecSqlStatementInProcedureDivisionContext ctx) {
    parseSql(ctx.execSqlStatement(), Db2SqlParser::procedureDivisionRules);
  }

  @Override
  public void exitExecSqlStatementInWorkingStorage(ExecSqlStatementInWorkingStorageContext ctx) {
    parseSql(ctx.execSqlStatement(), Db2SqlParser::dataDivisionRules);
  }

  @Override
  public void exitExecSqlStatementInWorkingStorageAndLinkageSection(
      ExecSqlStatementInWorkingStorageAndLinkageSectionContext ctx) {
    parseSql(ctx.execSqlStatement(), Db2SqlParser::dataDivisionRules);
  }

  @Override
  public void exitExecSqlStatementInDataDivision(ExecSqlStatementInDataDivisionContext ctx) {
    parseSql(ctx.execSqlStatement(), Db2SqlParser::dataDivisionRules);
  }

  @Override
  public void exitExecCicsStatement(ExecCicsStatementContext ctx) {
    parseCics(ctx.cicsRules());
  }

  private void parseCics(CicsRulesContext context) {
    CICSLexer lexer = new CICSLexer(CharStreams.fromString(VisitorHelper.getIntervalText(context)));
    lexer.removeErrorListeners();

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    embeddedCodeParts.put(
        context.getStart(),
        new EmbeddedCode(new CICSParser(tokens).allCicsRules(), tokens, calculateShift(context)));
  }

  private void parseSql(
      ExecSqlStatementContext context, Function<Db2SqlParser, ParserRuleContext> grammarStartRule) {

    SqlCodeContext sqlCode = context.sqlCode();
    CommonTokenStream tokens = applyDb2Lexer(sqlCode);
    embeddedCodeParts.put(
        sqlCode.getStart(),
        new EmbeddedCode(
            grammarStartRule.apply(createDb2SqlParser(tokens)), tokens, calculateShift(sqlCode)));
  }

  private CommonTokenStream applyDb2Lexer(ParserRuleContext context) {
    Db2SqlLexer lexer =
        new Db2SqlLexer(CharStreams.fromString(VisitorHelper.getIntervalText(context)));
    lexer.removeErrorListeners();

    return new CommonTokenStream(lexer);
  }

  private Db2SqlParser createDb2SqlParser(CommonTokenStream tokens) {
    Db2SqlParser parser = new Db2SqlParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(errorListener);
    parser.addParseListener(treeListener);
    parser.setErrorHandler(errorStrategy);
    return parser;
  }

  private int calculateShift(ParserRuleContext ctx) {
    Interval interval = ctx.getSourceInterval();
    return interval.b - interval.a;
  }
}
