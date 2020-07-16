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
 *   Broadcom, Inc. - initial API and implementation
 */
package com.ca.lsp.core.cobol.preprocessor.sub.document.impl;

import com.broadcom.lsp.domain.common.model.Position;
import com.ca.lsp.core.cobol.model.CopybookUsage;
import com.ca.lsp.core.cobol.model.ExtendedDocument;
import com.ca.lsp.core.cobol.model.ResultWithErrors;
import com.ca.lsp.core.cobol.parser.CobolPreprocessorLexer;
import com.ca.lsp.core.cobol.parser.CobolPreprocessorParser;
import com.ca.lsp.core.cobol.parser.CobolPreprocessorParser.StartRuleContext;
import com.ca.lsp.core.cobol.preprocessor.sub.document.GrammarPreprocessor;
import com.ca.lsp.core.cobol.preprocessor.sub.document.GrammarPreprocessorListener;
import com.ca.lsp.core.cobol.preprocessor.sub.document.GrammarPreprocessorListenerFactory;
import com.ca.lsp.core.cobol.preprocessor.sub.util.TokenUtils;
import com.google.inject.Inject;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import javax.annotation.Nonnull;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * This class runs pre-processing for COBOL using CobolPreprocessor.g4 grammar file. As a result, it
 * returns an extended document with all the available copybooks included, with their definitions
 * and usages specified, as well as related errors.
 */
public class GrammarPreprocessorImpl implements GrammarPreprocessor {
  private GrammarPreprocessorListenerFactory listenerFactory;
  private TokenUtils tokenUtils;

  @Inject
  public GrammarPreprocessorImpl(
      GrammarPreprocessorListenerFactory listenerFactory, TokenUtils tokenUtils) {
    this.listenerFactory = listenerFactory;
    this.tokenUtils = tokenUtils;
  }

  @Nonnull
  @Override
  public ResultWithErrors<ExtendedDocument> buildExtendedDocument(
      @Nonnull String uri,
      @Nonnull String code,
      @Nonnull Deque<CopybookUsage> copybookStack,
      @Nonnull String textDocumentSyncType) {
    // run the lexer
    CobolPreprocessorLexer lexer = new CobolPreprocessorLexer(CharStreams.fromString(code));
    lexer.removeErrorListeners();
    // get a list of matched tokens
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // pass the tokens to the parser
    CobolPreprocessorParser parser = new CobolPreprocessorParser(tokens);
    parser.removeErrorListeners();

    // specify our entry point
    StartRuleContext startRule = parser.startRule();

    ParseTreeWalker walker = new ParseTreeWalker();
    GrammarPreprocessorListener listener =
        listenerFactory.create(uri, tokens, copybookStack, textDocumentSyncType);
    walker.walk(listener, startRule);

    Map<String, List<Position>> innerMappings = listener.getDocumentMappings();
    List<Position> tokenMapping = tokenUtils.retrievePositionsFromText(uri, code);

    innerMappings.put(uri, tokenMapping);

    return new ResultWithErrors<>(
        new ExtendedDocument(listener.getResult(), listener.getCopybooks(), innerMappings),
        listener.getErrors());
  }
}