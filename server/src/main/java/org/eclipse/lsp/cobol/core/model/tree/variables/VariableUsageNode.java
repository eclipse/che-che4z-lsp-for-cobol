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
package org.eclipse.lsp.cobol.core.model.tree.variables;

import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.lsp.cobol.core.model.Locality;
import org.eclipse.lsp.cobol.core.model.SyntaxError;
import org.eclipse.lsp.cobol.core.model.tree.Node;
import org.eclipse.lsp.cobol.core.model.tree.NodeType;
import org.eclipse.lsp.cobol.core.model.tree.ProgramNode;

import java.util.List;

/**
 * The class represents variable usage in COBOL program. This must be extended with a link to
 * variable definition.
 */
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class VariableUsageNode extends Node {
  private final String dataName;
  private final List<VariableNameAndLocality> parents;
  private final ReferenceType type;

  public VariableUsageNode(
          String dataName,
          Locality locality,
          List<VariableNameAndLocality> parents) {
    this(dataName, locality, parents, ReferenceType.STRUCTURE);
  }

  public VariableUsageNode(
      String dataName,
      Locality locality,
      List<VariableNameAndLocality> parents,
      ReferenceType type) {
    super(locality, NodeType.VARIABLE_USAGE);
    this.dataName = dataName;
    this.parents = parents;
    this.type = type;
  }

  @Override
  public List<SyntaxError> process() {
    getNearestParentByType(NodeType.PROGRAM)
        .map(ProgramNode.class::cast)
        .map(ProgramNode::getVariableUsageDelegate)
        .ifPresent(
            variableUsageDelegate ->
                variableUsageDelegate.handleUsage(dataName, getLocality(), parents, type));

    return ImmutableList.of();
  }

  /**
   * This enum determines the type of referencing the variable in the original document. E.g.
   * STRUCTURE if the variable is referenced by its hierarchy (default case) or CONTEXT if the
   * variable structure is clear from the context and don't have to be specified explicitly
   */
  public enum ReferenceType {
    STRUCTURE,
    CONTEXT
  }
}
