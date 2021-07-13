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
package org.eclipse.lsp.cobol.core.model.tree;

import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.lsp.cobol.core.messages.MessageService;
import org.eclipse.lsp.cobol.core.model.Locality;
import org.eclipse.lsp.cobol.core.model.SyntaxError;
import org.eclipse.lsp.cobol.core.model.tree.statements.StatementNode;
import org.eclipse.lsp.cobol.core.model.tree.variables.VariableNode;
import org.eclipse.lsp.cobol.core.model.variables.NodeConverter;
import org.eclipse.lsp.cobol.core.model.variables.Variable;
import org.eclipse.lsp.cobol.core.visitor.VariableUsageDelegate;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.lsp.cobol.core.model.tree.NodeType.PROGRAM;
import static org.eclipse.lsp.cobol.core.model.tree.NodeType.STATEMENT;

/** This class represents program context in COBOL. */
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class ProgramNode extends Node {
  private final Collection<Variable> definedVariables = new ArrayList<>();
  @EqualsAndHashCode.Exclude private final NodeConverter nodeConverter = new NodeConverter();
  @EqualsAndHashCode.Exclude private VariableUsageDelegate variableUsageDelegate;
  private String programName;

  /**
   * Use for testing.
   *
   * @param locality the node location.
   */
  ProgramNode(Locality locality) {
    super(locality, PROGRAM);
  }

  public ProgramNode(Locality locality, MessageService messageService) {
    super(locality, PROGRAM);
    variableUsageDelegate = new VariableUsageDelegate(messageService);
  }

  public VariableUsageDelegate getVariableUsageDelegate() {
    return variableUsageDelegate;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  @Override
  public List<SyntaxError> process() {
    List<SyntaxError> errors = super.process();
    Set<String> variableNames = definedVariables.stream().map(Variable::getName).collect(toSet());

    List<Variable> availableVariables = new ArrayList<>(definedVariables);
    getGlobalVariables().stream()
        .filter(variable -> !variableNames.contains(variable.getName()))
        .forEach(availableVariables::add);
    Map<Locality, Variable> variableUsages =
        variableUsageDelegate
            .updateUsageAndGenerateErrors(availableVariables)
            .unwrap(errors::addAll);
    errors.addAll(validateStatements(variableUsages));
    return errors;
  }

  public Collection<Variable> getDefinedVariables() {
    return definedVariables;
  }

  /**
   * Add the variable definition to that program context.
   *
   * @param node the variable definition node
   */
  public void addVariableDefinition(VariableNode node) {
    definedVariables.add(nodeConverter.convertVariable(node));
  }

  private List<Variable> getGlobalVariables() {
    List<Variable> globalVariables =
        definedVariables.stream().filter(Variable::isGlobal).collect(toList());
    Set<String> globalVariablesNames =
        globalVariables.stream().map(Variable::getName).collect(toSet());
    getNearestParentByType(PROGRAM)
        .map(ProgramNode.class::cast)
        .map(ProgramNode::getGlobalVariables)
        .orElseGet(ImmutableList::of)
        .stream()
        .filter(variable -> !globalVariablesNames.contains(variable.getName()))
        .forEach(globalVariables::add);
    return globalVariables;
  }

  private List<SyntaxError> validateStatements(Map<Locality, Variable> variables) {
    return getDepthFirstStream()
        .filter(hasType(STATEMENT))
        .map(StatementNode.class::cast)
        .map(it -> it.validate(variables))
        .flatMap(Collection::stream)
        .collect(toList());
  }
}
