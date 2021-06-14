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

package org.eclipse.lsp.cobol.core.grammarcheck;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.lsp.cobol.core.ANTLRv4Parser;
import org.eclipse.lsp.cobol.core.ANTLRv4ParserBaseListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** asdf */
@AllArgsConstructor
public class GrammarCheckListener extends ANTLRv4ParserBaseListener {
  @Getter private final Map<String, String> keywords = new HashMap<>();
  @Getter private final Map<String, RuleDefinition> rules = new HashMap<>();
  private static final String KEYWORD = "\\b[A-Z][A-Za-z0-9_]*\\b";
  private static final String RULE = "\\b[a-z][A-Za-z0-9_]*\\b";
  private final CommonTokenStream tokens;

  @Override
  public void exitLexerRuleSpec(ANTLRv4Parser.LexerRuleSpecContext ctx) {
    final String name = tokens.getText(ctx.lexerRuleName());
    if (!name.startsWith("fragment")) keywords.put(name, tokens.getText(ctx.lexerRuleBlock()));
  }

  @Override
  public void exitParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
    rules.put(tokens.getText(ctx.parserRuleName()), retrieveRuleDefinition(ctx));
  }

  private RuleDefinition retrieveRuleDefinition(ANTLRv4Parser.ParserRuleSpecContext ctx) {
    final String text = tokens.getText(ctx);
    return new RuleDefinition(parse(text, KEYWORD), parse(text, RULE));
  }

  private Set<String> parse(String text, String pattern) {
    final Matcher matcher = Pattern.compile(pattern).matcher(text);
    Set<String> result = new HashSet<>();
    while (matcher.find()) {
      result.add(matcher.group());
    }
    return result;
  }
}
