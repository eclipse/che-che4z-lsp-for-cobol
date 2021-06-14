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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp.cobol.core.ANTLRv4Lexer;
import org.eclipse.lsp.cobol.core.ANTLRv4Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** asdfsdf */
public class GrammarCheckEngine {

  public static final String PACKAGE =
      "C:\\workspace\\che-che4z-lsp-for-cobol\\server\\src\\main\\antlr4\\org\\eclipse\\lsp\\cobol\\core\\parser\\";

  /** asdf */
  public void analyze() {
    analyze("CobolLexer.g4", "CobolParser.g4");
  }

  private void analyze(String lexerName, String parserName) {
    Map<String, String> lexerTokens = loadLexer(lexerName, GrammarCheckListener::getKeywords);
    Map<String, String> techTokens =
        loadLexer("TechnicalLexer.g4", GrammarCheckListener::getKeywords);

    //        checkDuplicatedTokens(lexerTokens, techTokens);
    Map<String, String> concatenatedTokens = new HashMap<>(lexerTokens);
    concatenatedTokens.putAll(techTokens);

    Map<String, RuleDefinition> parserRules = loadLexer(parserName, GrammarCheckListener::getRules);

    analyzeTokens(lexerName, concatenatedTokens, lexerTokens, parserRules);
  }
  //
  //  private void checkDuplicatedTokens(
  //      Map<String, String> cicsTokens, Map<String, String> techTokens) {
  //    System.out.println("=============Duplicated tokens=================");
  //    Stream.concat(
  //            cicsTokens.keySet().stream().filter(techTokens::containsKey),
  //            techTokens.keySet().stream().filter(cicsTokens::containsKey))
  //        .forEach(System.out::println);
  //  }

  private void analyzeTokens(
      String lexerName,
      Map<String, String> allTokens,
      Map<String, String> specificTokens,
      Map<String, RuleDefinition> parserRules) {
    final List<String> parserKeywords = extractKeywordsInParser(parserRules);
    final List<String> unusedKeywords = collectUnusedKeywords(specificTokens, parserKeywords);
    final List<String> implicitKeywords = collectImplicitKeywords(allTokens, parserKeywords);
    System.out.println("==============Unused keywords====================");
    unusedKeywords.forEach(System.out::println);
    System.out.println("==============Implicitly defined keywords======================");
    implicitKeywords.forEach(System.out::println);
    deleteTokens(lexerName, unusedKeywords);
  }

  private void deleteTokens(String fileName, List<String> tokensToDelete) {
    List<String> cleanContent;
    final Path path = Paths.get(PACKAGE + fileName);
    try (Stream<String> lines = Files.lines(path)) {
      cleanContent =
          lines
              .filter(line -> tokensToDelete.stream().noneMatch(line::startsWith))
              .collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
      cleanContent = new ArrayList<>();
    }
    cleanContent.forEach(System.out::println);
    try {
      Files.write(path, cleanContent);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private List<String> extractKeywordsInParser(Map<String, RuleDefinition> cicsRules) {
    return cicsRules.values().stream()
        .map(RuleDefinition::getKeywords)
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());
  }

  private List<String> collectUnusedKeywords(
      Map<String, String> lexerTokens, List<String> parserKeywords) {
    return lexerTokens.keySet().stream()
        .filter(it -> !parserKeywords.contains(it))
        .collect(Collectors.toList());
  }

  private List<String> collectImplicitKeywords(
      Map<String, String> cicsTokens, List<String> parserKeywords) {
    return parserKeywords.stream()
        .filter(it -> !cicsTokens.containsKey(it))
        .collect(Collectors.toList());
  }

  private <T> T loadLexer(String name, Function<GrammarCheckListener, T> retriever) {
    ANTLRv4Lexer lexer = new ANTLRv4Lexer(CharStreams.fromString(load(name)));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
    GrammarCheckListener listener = new GrammarCheckListener(tokens);
    new ParseTreeWalker().walk(listener, parser.grammarSpec());
    return retriever.apply(listener);
  }

  private String load(String file) {
    try (Stream<String> lines = Files.lines(Paths.get(PACKAGE + file))) {
      return lines.collect(Collectors.joining("\n"));
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
  }
}
