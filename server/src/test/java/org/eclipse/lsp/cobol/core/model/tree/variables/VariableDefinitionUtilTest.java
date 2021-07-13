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
import org.eclipse.lsp.cobol.core.model.Locality;
import org.eclipse.lsp.cobol.core.model.tree.Node;
import org.eclipse.lsp.cobol.core.model.tree.NodeType;
import org.eclipse.lsp.cobol.core.model.tree.ProgramNode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.lsp.cobol.core.model.tree.Node.hasType;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test {@link VariableDefinitionUtil} */
class VariableDefinitionUtilTest {
  private static final Locality LOCALITY = Locality.builder().build();

  @Test
  void simpleStructureTest() {
    Node programNode = new ProgramNode(LOCALITY, null);
    // 01 Level-01-order-1
    // 01 Level-01-order-2
    //    05 Level-05-order-1
    //       10 Level-10-order-1
    //    05 Level-05-order-2
    // 01 Level-01-order-3
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(1)
            .variableName(new VariableNameAndLocality("Level-01-order-1", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(1)
            .variableName(new VariableNameAndLocality("Level-01-order-2", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(5)
            .variableName(new VariableNameAndLocality("Level-05-order-1", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(10)
            .variableName(new VariableNameAndLocality("Level-10-order-1", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(5)
            .variableName(new VariableNameAndLocality("Level-05-order-2", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(1)
            .variableName(new VariableNameAndLocality("Level-01-order-3", LOCALITY))
            .build());
    VariableDefinitionUtil.processNodeWithVariableDefinitions(programNode);
    List<VariableNode> nodesLevel01 = getVariables(programNode);
    checkNames(nodesLevel01, "Level-01-order-1", "Level-01-order-2", "Level-01-order-3");
    List<VariableNode> nodesLevel05 = getVariables(nodesLevel01.get(1));
    checkNames(nodesLevel05, "Level-05-order-1", "Level-05-order-2");
    List<VariableNode> nodesLevel10 = getVariables(nodesLevel05.get(0));
    checkNames(nodesLevel10, "Level-10-order-1");
  }

  @Test
  void nonCorrectStructures() {
    Node programNode = new ProgramNode(LOCALITY, null);
    // 05 Level-05
    // 01 Level-01
    //   10 Level-10
    //   07 Level-07
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(5)
            .variableName(new VariableNameAndLocality("Level-05", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(1)
            .variableName(new VariableNameAndLocality("Level-01", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(10)
            .variableName(new VariableNameAndLocality("Level-10", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(7)
            .variableName(new VariableNameAndLocality("Level-07", LOCALITY))
            .build());
    VariableDefinitionUtil.processNodeWithVariableDefinitions(programNode);
    List<VariableNode> nodesLowLevel = getVariables(programNode);
    checkNames(nodesLowLevel, "Level-05", "Level-01");
    List<VariableNode> nodesNestedLevel = getVariables(nodesLowLevel.get(1));
    checkNames(nodesNestedLevel, "Level-10", "Level-07");
  }

  @Test
  void moveLevel66ToTop() {
    Node programNode = new ProgramNode(LOCALITY, null);
    // 01 Level-01
    //    05 Level-05
    // 66 Level-66
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(1)
            .variableName(new VariableNameAndLocality("Level-01", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(5)
            .variableName(new VariableNameAndLocality("Level-05", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(66)
            .variableName(new VariableNameAndLocality("Level-66", LOCALITY))
            .build());
    VariableDefinitionUtil.processNodeWithVariableDefinitions(programNode);
    List<VariableNode> nodesLevel01 = getVariables(programNode);
    checkNames(nodesLevel01, "Level-01", "Level-66");
  }

  @Test
  void level88() {
    List<ValueClause> valueClauses =
        ImmutableList.of(new ValueClause(ImmutableList.of(new ValueInterval("", "")), LOCALITY));
    Node programNode = new ProgramNode(LOCALITY, null);
    // 01 Level-01
    //      88 Cond-1
    //    05 Level-05-1
    //      88 Cond-2
    //    05 Level-05-1
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(1)
            .variableName(new VariableNameAndLocality("Level-01", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(88)
            .variableName(new VariableNameAndLocality("Cond-1", LOCALITY))
            .valueClauses(valueClauses)
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(5)
            .variableName(new VariableNameAndLocality("Level-05-1", LOCALITY))
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(88)
            .variableName(new VariableNameAndLocality("Cond-2", LOCALITY))
            .valueClauses(valueClauses)
            .build());
    programNode.addChild(
        VariableDefinitionNode.builder()
            .level(5)
            .variableName(new VariableNameAndLocality("Level-05-2", LOCALITY))
            .build());
    VariableDefinitionUtil.processNodeWithVariableDefinitions(programNode);
    List<VariableNode> nodesLevel01 = getVariables(programNode);
    checkNames(nodesLevel01, "Level-01");
    List<VariableNode> nodesUnder01 = getVariables(nodesLevel01.get(0));
    checkNames(nodesUnder01, "Cond-1", "Level-05-1", "Level-05-2");
  }

  private static List<VariableNode> getVariables(Node node) {
    return node.getChildren().stream()
        .filter(hasType(NodeType.VARIABLE))
        .map(VariableNode.class::cast)
        .collect(Collectors.toList());
  }

  private static void checkNames(List<VariableNode> nodes, String... names) {
    List<String> actualNames =
        nodes.stream().map(VariableNode::getName).collect(Collectors.toList());
    List<String> expectedNames = Arrays.asList(names);
    assertEquals(expectedNames, actualNames);
  }
}
