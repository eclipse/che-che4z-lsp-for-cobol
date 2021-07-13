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

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.RuleNode;
import org.eclipse.lsp.cobol.core.Db2SqlParserBaseVisitor;
import org.eclipse.lsp.cobol.core.model.Locality;
import org.eclipse.lsp.cobol.core.model.tree.Node;
import org.eclipse.lsp.cobol.core.model.tree.variables.VariableUsageNode;
import org.eclipse.lsp.cobol.core.semantics.PredefinedVariableContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.eclipse.lsp.cobol.core.Db2SqlParser.Dbs_host_variableContext;
import static org.eclipse.lsp.cobol.core.Db2SqlParser.Dbs_rs_locator_variableContext;
import static org.eclipse.lsp.cobol.core.visitor.VisitorHelper.*;

/**
 * This visitor analyzes the parser tree for DB2 SQL and returns its semantic context as a syntax
 * tree
 */
@Slf4j
@AllArgsConstructor
public class Db2SqlVisitor extends Db2SqlParserBaseVisitor<List<Node>> {
  private final Map<Token, Locality> positions;
  private final PredefinedVariableContext constants;

  @Override
  public List<Node> visitDbs_host_variable(Dbs_host_variableContext ctx) {
    return ofNullable(ctx.dbs_host_variable_val())
        .map(it -> extractVariableNodes(ctx, it))
        .orElse(visitChildren(ctx));
  }

  @Override
  public List<Node> visitDbs_rs_locator_variable(Dbs_rs_locator_variableContext ctx) {
    return ofNullable(ctx.dbs_sql_identifier())
        .map(it -> extractVariableNodes(ctx, it))
        .orElse(visitChildren(ctx));
  }

  private List<Node> extractVariableNodes(
      ParserRuleContext parentCtx, ParserRuleContext variableCtx) {
    if (variableCtx == null) return visitChildren(parentCtx);

    String dataName = getName(variableCtx);
    return getLocality(variableCtx.getStart())
        .map(
            locality -> {
              if (constants.contains(dataName)) {
                constants.addUsage(dataName, locality.toLocation());
                return visitChildren(parentCtx);
              } else
                return addTreeNode(
                    variableCtx,
                    loc ->
                        new VariableUsageNode(
                            dataName, locality, ImmutableList.of()));
            })
        .orElseGet(() -> visitChildren(parentCtx));
  }

  // NOTE: Visitor is not managed by Guice DI, so can't use annotation here.
  @Override
  public List<Node> visitChildren(RuleNode node) {
    checkInterruption();
    return super.visitChildren(node);
  }

  @Override
  protected List<Node> defaultResult() {
    return ImmutableList.of();
  }

  @Override
  protected List<Node> aggregateResult(List<Node> aggregate, List<Node> nextResult) {
    return Stream.concat(aggregate.stream(), nextResult.stream()).collect(toList());
  }

  private Optional<Locality> getLocality(Token childToken) {
    return ofNullable(positions.get(childToken));
  }

  private List<Node> addTreeNode(ParserRuleContext ctx, Function<Locality, Node> nodeConstructor) {
    List<Node> children = visitChildren(ctx);
    return retrieveRangeLocality(ctx, positions)
        .map(constructNode(nodeConstructor, children))
        .orElse(children);
  }
}
