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
package org.eclipse.lsp.cobol.core.visitor;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp.cobol.core.messages.MessageService;
import org.eclipse.lsp.cobol.core.model.*;
import org.eclipse.lsp.cobol.core.model.tree.variables.VariableNameAndLocality;
import org.eclipse.lsp.cobol.core.model.tree.variables.VariableUsageNode;
import org.eclipse.lsp.cobol.core.model.variables.Variable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * This class processes the variable usage contexts. It accumulates usages when they appear in the
 * text. Later it checks variable definition and updates its usages.
 */
@Slf4j
@RequiredArgsConstructor
public class VariableUsageDelegate {

  private final List<VariableUsage> variableUsages = new ArrayList<>();
  private final MessageService messageService;

  /**
   * Accumulate variable appearance for analise its definition later.
   *
   * @param dataName the variable name
   * @param locality the variable text position
   * @param parents the list of variable parents
   * @param type the referencing type of the varaible
   */
  public void handleUsage(
      String dataName,
      Locality locality,
      List<VariableNameAndLocality> parents,
      VariableUsageNode.ReferenceType type) {
    variableUsages.add(new VariableUsage(dataName, parents, locality, type));
  }

  /**
   * Find correct definition for collected usages. Update variable usage or produce an error.
   *
   * @param definedVariables the collection of COBOL variables
   * @return the list of usage errors
   */
  public ResultWithErrors<Map<Locality, Variable>> updateUsageAndGenerateErrors(
      Collection<Variable> definedVariables) {
    Map<Locality, Variable> variablesByUsages = new HashMap<>();
    Map<String, List<Variable>> convertedVariables =
        VariableUsageUtils.convertDefinedVariables(definedVariables);
    List<SyntaxError> errors = new ArrayList<>();
    for (VariableUsage variableUsage : variableUsages) {
      List<Variable> foundVariables =
          VariableUsageUtils.findVariables(
              convertedVariables,
              variableUsage.name,
              variableUsage.parents.stream()
                  .map(VariableNameAndLocality::getName)
                  .collect(Collectors.toList()));
      if (foundVariables.size() == 1) {
        Variable variable = foundVariables.get(0);
        variable.addUsage(variableUsage.locality);
        variablesByUsages.put(variableUsage.locality, variable);
        if (variableUsage.type == VariableUsageNode.ReferenceType.STRUCTURE)
          addParentVariablesUsage(variableUsage.parents, variable);
      } else {
        String message = foundVariables.isEmpty() ? "semantics.notDefined" : "semantics.duplicated";
        errors.add(createInvalidDefinition(variableUsage.name, variableUsage.locality, message));
      }
    }
    return new ResultWithErrors<>(variablesByUsages, errors);
  }

  private void addParentVariablesUsage(
      List<VariableNameAndLocality> parentVariables, Variable variable) {
    Map<String, Locality> parentUsages =
        parentVariables.stream()
            .collect(
                Collectors.toMap(
                    VariableNameAndLocality::getName, VariableNameAndLocality::getLocality));
    Variable parent = variable.getParent();
    while (parent != null) {
      ofNullable(parentUsages.get(parent.getName())).ifPresent(parent::addUsage);
      parent = parent.getParent();
    }
  }

  private SyntaxError createInvalidDefinition(String dataName, Locality locality, String message) {
    SyntaxError error =
        SyntaxError.syntaxError()
            .suggestion(messageService.getMessage(message, dataName))
            .severity(ErrorSeverity.ERROR)
            .locality(locality)
            .build();
    LOG.debug("Syntax error by VariableUsageDelegate#createInvalidDefinition: " + error.toString());
    return error;
  }

  /** This value class used for temporal storage variable usages. */
  @Value
  private static class VariableUsage {
    String name;
    List<VariableNameAndLocality> parents;
    Locality locality;
    VariableUsageNode.ReferenceType type;
  }
}
