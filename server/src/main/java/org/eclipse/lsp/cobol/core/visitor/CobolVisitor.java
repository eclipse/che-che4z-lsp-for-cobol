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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp.cobol.core.CobolParser;
import org.eclipse.lsp.cobol.core.CobolParserBaseVisitor;
import org.eclipse.lsp.cobol.core.messages.MessageService;
import org.eclipse.lsp.cobol.core.model.*;
import org.eclipse.lsp.cobol.core.model.tree.*;
import org.eclipse.lsp.cobol.core.model.tree.statements.SetToBooleanStatement;
import org.eclipse.lsp.cobol.core.model.tree.statements.SetToOnOffStatement;
import org.eclipse.lsp.cobol.core.model.tree.statements.SetUpDownByStatement;
import org.eclipse.lsp.cobol.core.model.tree.variables.*;
import org.eclipse.lsp.cobol.core.model.tree.variables.VariableDefinitionNode.Builder;
import org.eclipse.lsp.cobol.core.preprocessor.delegates.util.PreprocessorStringUtils;
import org.eclipse.lsp.cobol.core.semantics.GroupContext;
import org.eclipse.lsp.cobol.core.semantics.NamedSubContext;
import org.eclipse.lsp.cobol.core.semantics.PredefinedVariableContext;
import org.eclipse.lsp.cobol.core.semantics.SemanticContext;
import org.eclipse.lsp.cobol.core.semantics.outline.NodeType;
import org.eclipse.lsp.cobol.core.semantics.outline.OutlineTreeBuilder;
import org.eclipse.lsp.cobol.service.SubroutineService;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.lsp.cobol.core.CobolParser.*;
import static org.eclipse.lsp.cobol.core.model.tree.variables.VariableDefinitionUtil.*;
import static org.eclipse.lsp.cobol.core.semantics.outline.OutlineNodeNames.*;
import static org.eclipse.lsp.cobol.core.visitor.VisitorHelper.*;

/**
 * This extension of {@link CobolParserBaseVisitor} applies the semantic analysis based on the
 * abstract syntax tree built by {@link CobolParser}. It requires a semantic context with defined
 * elements to add the usages or throw a warning on an invalid definition. If there is a misspelled
 * keyword, the visitor finds it and throws a warning.
 */
@Slf4j
public class CobolVisitor extends CobolParserBaseVisitor<List<Node>> {

  private final List<SyntaxError> errors = new ArrayList<>();
  private final PredefinedVariableContext constants = new PredefinedVariableContext();
  private final GroupContext groupContext = new GroupContext();
  private final Multimap<String, Location> subroutineUsages = HashMultimap.create();
  private final NamedSubContext copybooks;
  private final CommonTokenStream tokenStream;
  private final OutlineTreeBuilder outlineTreeBuilder;
  private final Map<Token, Locality> positionMapping;
  private final Map<Token, EmbeddedCode> embeddedCodeParts;
  private final MessageService messageService;
  private final SubroutineService subroutineService;

  public CobolVisitor(
      @NonNull String documentUri,
      @NonNull NamedSubContext copybooks,
      @NonNull CommonTokenStream tokenStream,
      @NonNull Map<Token, Locality> positionMapping,
      Map<Token, EmbeddedCode> embeddedCodeParts,
      MessageService messageService,
      SubroutineService subroutineService) {
    this.copybooks = copybooks;
    this.positionMapping = positionMapping;
    this.embeddedCodeParts = embeddedCodeParts;
    this.tokenStream = tokenStream;
    this.messageService = messageService;
    this.subroutineService = subroutineService;
    outlineTreeBuilder = new OutlineTreeBuilder(documentUri, positionMapping);
  }

  /**
   * Get the semantic context of the document as an output of th semantic analysis
   *
   * @return the semantic context of the document, containing all the definitions and usages of
   *     paragraphs, variables and copybooks
   */
  @NonNull
  public ResultWithErrors<SemanticContext> finishAnalysis() {
    errors.addAll(groupContext.generateParagraphErrors(messageService));
    return new ResultWithErrors<>(
        SemanticContext.builder()
            .paragraphDefinitions(groupContext.getParagraphDefinitions())
            .paragraphUsages(groupContext.getParagraphUsages())
            .paragraphRanges(groupContext.getParagraphRanges())
            .sectionDefinitions(groupContext.getSectionDefinitions())
            .sectionUsages(groupContext.getSectionUsages())
            .sectionRanges(groupContext.getSectionRanges())
            .constants(constants)
            .copybookDefinitions(copybooks.getDefinitions().asMap())
            .copybookUsages(copybooks.getUsages().asMap())
            .subroutinesDefinitions(getSubroutineDefinition())
            .subroutinesUsages(subroutineUsages.asMap())
            .outlineTree(buildOutlineTree())
            .build(),
        errors);
  }

  @Override
  public List<Node> visitStartRule(StartRuleContext ctx) {
    // we can skip the other nodes, but not the root
    return retrieveRangeLocality(ctx, positionMapping)
        .map(
            locality -> {
              Node rootNode = new RootNode(locality);
              visitChildren(ctx).forEach(rootNode::addChild);
              return ImmutableList.of(rootNode);
            })
        .orElse(ImmutableList.of());
  }

  @Override
  public List<Node> visitIdentificationDivision(IdentificationDivisionContext ctx) {
    areaAWarning(ctx.getStart());
    outlineTreeBuilder.addNode(IDENTIFICATION_DIVISION, NodeType.DIVISION, ctx);
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitProgramIdParagraph(ProgramIdParagraphContext ctx) {
    List<Node> result = new ArrayList<>();
    ofNullable(ctx.programName())
        .map(RuleContext::getText)
        .map(PreprocessorStringUtils::trimQuotes)
        .ifPresent(
            name -> {
              retrieveRangeLocality(ctx, positionMapping)
                  .ifPresent(locality -> result.add(new ProgramIdNode(locality, name)));
              outlineTreeBuilder.renameProgram(name, ctx);
              outlineTreeBuilder.addNode(PROGRAM_ID_PREFIX + name, NodeType.PROGRAM_ID, ctx);
            });
    return result;
  }

  @Override
  public List<Node> visitProcedureDivision(ProcedureDivisionContext ctx) {
    areaAWarning(ctx.getStart());
    outlineTreeBuilder.addNode(PROCEDURE_DIVISION, NodeType.DIVISION, ctx);
    return addTreeNode(ctx, ProcedureDivisionNode::new);
  }

  @Override
  public List<Node> visitEnvironmentDivision(EnvironmentDivisionContext ctx) {
    areaAWarning(ctx.getStart());
    outlineTreeBuilder.addNode(ENVIRONMENT_DIVISION, NodeType.DIVISION, ctx);
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitDataDivision(DataDivisionContext ctx) {
    areaAWarning(ctx.getStart());
    outlineTreeBuilder.addNode(DATA_DIVISION, NodeType.DIVISION, ctx);
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitDataDivisionSection(DataDivisionSectionContext ctx) {
    areaAWarning(ctx.getStart());
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitEnvironmentDivisionBody(EnvironmentDivisionBodyContext ctx) {
    areaAWarning(ctx.getStart());
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitProcedureSectionHeader(ProcedureSectionHeaderContext ctx) {
    areaAWarning(ctx.getStart());
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitWorkingStorageSection(WorkingStorageSectionContext ctx) {
    outlineTreeBuilder.addNode(WORKING_STORAGE_SECTION, NodeType.SECTION, ctx);
    outlineTreeBuilder.initVariables();
    return addTreeNode(ctx, SectionNode::new);
  }

  @Override
  public List<Node> visitProgramUnit(ProgramUnitContext ctx) {
    outlineTreeBuilder.addProgram(ctx);
    return addTreeNode(ctx, locality -> new ProgramNode(locality, messageService));
  }

  @Override
  public List<Node> visitProcedureSection(ProcedureSectionContext ctx) {
    throwWarning(ctx.getStart());
    addSectionRange(ctx);
    outlineTreeBuilder.addNode(ctx.getStart().getText(), NodeType.PROCEDURE_SECTION, ctx);

    String name = ctx.getStart().getText().toUpperCase();
    getLocality(ctx.getStart())
        .ifPresent(locality -> groupContext.addSectionDefinition(name, locality));
    return addTreeNode(
        ctx,
        locality ->
            new ProcedureSectionNode(locality, ctx.getStart().getText(), getIntervalText(ctx)));
  }

  @Override
  public List<Node> visitParagraph(ParagraphContext ctx) {
    areaAWarning(ctx.getStart());
    addParagraphRange(ctx);

    outlineTreeBuilder.addNode(ctx.getStart().getText(), NodeType.PROCEDURE, ctx);
    String name = ctx.paragraphName().getText();
    return addTreeNode(ctx, locality -> new ParagraphNode(locality, name, getIntervalText(ctx)));
  }

  private void addSectionRange(ParserRuleContext ctx) {
    String name = ctx.getStart().getText().toUpperCase();
    getRange(ctx).ifPresent(range -> groupContext.addSectionRange(name, range));
  }

  private void addParagraphRange(ParserRuleContext ctx) {
    String name = ctx.getStart().getText().toUpperCase();
    getRange(ctx).ifPresent(range -> groupContext.addParagraphRange(name, range));
  }

  private Optional<Location> getRange(ParserRuleContext ctx) {
    Optional<Locality> start = getLocality(ctx.getStart());
    Optional<Locality> end = getLocality(ctx.getStop());

    if (start.isPresent() && end.isPresent()) {
      Range range = new Range(start.get().getRange().getStart(), end.get().getRange().getEnd());
      return Optional.of(new Location(start.get().getUri(), range));
    }
    return Optional.empty();
  }

  @Override
  public List<Node> visitFileDescriptionEntry(FileDescriptionEntryContext ctx) {
    areaAWarning(ctx.getStart());
    outlineTreeBuilder.addNode(ctx.fileName().getText(), NodeType.FILE, ctx);
    return visitChildren(ctx);
  }

  /**
   * In this method, first condition is checking if there is any other element present on the same
   * line as DECLARATIVES token and throws an error if the condition is true; In the PROCEDURE
   * DIVISION, each of the keywords DECLARATIVES and END DECLARATIVES must begin in Area A and be
   * followed immediately by a separator period; no other text can appear on the same line. After
   * the keywords END DECLARATIVES, no text can appear before the following section header.
   */
  @Override
  public List<Node> visitProcedureDeclaratives(ProcedureDeclarativesContext ctx) {
    Token firstDeclarative = ctx.getStart();
    int firstDeclLine = firstDeclarative.getLine();
    Token declarativeBody = ctx.procedureDeclarative(0).getStart();
    Token endToken = ctx.END().getSymbol();

    if (firstDeclLine == declarativeBody.getLine()) {
      getLocality(declarativeBody)
          .ifPresent(
              locality ->
                  throwException(
                      declarativeBody.getText(),
                      locality,
                      messageService.getMessage("CobolVisitor.declarativeSameMsg")));
    }

    areaAWarning(firstDeclarative);
    areaAWarning(endToken);
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitEndProgramStatement(EndProgramStatementContext ctx) {
    Token endProgramNameToken = ctx.programName().getStart();
    String id = PreprocessorStringUtils.trimQuotes(endProgramNameToken.getText());
    areaAWarning(ctx.getStart());
    return addTreeNode(ctx.programName(), locality -> new ProgramEndNode(locality, id));
  }

  @Override
  public List<Node> visitConfigurationSection(ConfigurationSectionContext ctx) {
    outlineTreeBuilder.addNode(CONFIGURATION_SECTION, NodeType.SECTION, ctx);
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitInputOutputSection(InputOutputSectionContext ctx) {
    outlineTreeBuilder.addNode(INPUT_OUTPUT_SECTION, NodeType.SECTION, ctx);
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitSelectClause(SelectClauseContext ctx) {
    outlineTreeBuilder.addNode(ctx.fileName().getText(), NodeType.FILE, ctx);
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitFileSection(FileSectionContext ctx) {
    outlineTreeBuilder.addNode(FILE_SECTION, NodeType.SECTION, ctx);
    outlineTreeBuilder.initVariables();
    return addTreeNode(ctx, SectionNode::new);
  }

  @Override
  public List<Node> visitLinkageSection(LinkageSectionContext ctx) {
    outlineTreeBuilder.addNode(LINKAGE_SECTION, NodeType.SECTION, ctx);
    outlineTreeBuilder.initVariables();
    return addTreeNode(ctx, SectionNode::new);
  }

  @Override
  public List<Node> visitLocalStorageSection(LocalStorageSectionContext ctx) {
    outlineTreeBuilder.addNode(LOCAL_STORAGE_SECTION, NodeType.SECTION, ctx);
    outlineTreeBuilder.initVariables();
    return addTreeNode(ctx, SectionNode::new);
  }

  @Override
  public List<Node> visitStatement(StatementContext ctx) {
    areaBWarning(ctx);
    throwWarning(ctx.getStart());
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitExecCicsStatement(ExecCicsStatementContext ctx) {
    areaBWarning(ctx);
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitExecSqlStatement(ExecSqlStatementContext ctx) {
    areaBWarning(ctx);
    EmbeddedCode code = embeddedCodeParts.get(ctx.sqlCode().getStart());
    // apply area B check for tokens provided by a specific lexer
    areaBWarning(code.getTokens());
    return getLocality(ctx.getStart())
        .<List<Node>>map(
            it ->
                ImmutableList.of(
                    new EmbeddedCodeNode(
                        it, code.getTokenStream(), code.getTree(), EmbeddedCodeNode.Language.SQL)))
        .orElse(ImmutableList.of());
  }

  @Override
  public List<Node> visitIfThen(IfThenContext ctx) {
    throwWarning(ctx.getStart());
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitIfElse(IfElseContext ctx) {
    throwWarning(ctx.getStart());
    return addTreeNode(ctx, IfElseNode::new);
  }

  @Override
  public List<Node> visitPerformStatement(PerformStatementContext ctx) {
    final PerformProcedureStatementContext procStatement = ctx.performProcedureStatement();
    final PerformInlineStatementContext inlineStatement = ctx.performInlineStatement();
    if (procStatement != null) {
      ProcedureNameContext procedureNameContext = procStatement.procedureName().get(0);
      final String section =
          procedureNameContext.inSection() != null
              ? procedureNameContext.inSection().sectionName().getText()
              : null;
      final String targetName = procedureNameContext.paragraphNameUsage().getText();
      return addTreeNode(ctx, locality -> new PerformNode(locality, section, targetName));
    }
    if (inlineStatement != null) {
      return addTreeNode(ctx, PerformNode::new);
    }
    return super.visitPerformStatement(ctx);
  }

  @Override
  public List<Node> visitPerformInlineStatement(PerformInlineStatementContext ctx) {
    throwWarning(ctx.getStart());
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitSentence(SentenceContext ctx) {
    throwWarning(ctx.getStart());
    return addTreeNode(ctx, SentenceNode::new);
  }

  @Override
  public List<Node> visitEvaluateWhenOther(EvaluateWhenOtherContext ctx) {
    throwWarning(ctx.getStart());
    return addTreeNode(ctx, EvaluateWhenOtherNode::new);
  }

  @Override
  public List<Node> visitParagraphName(ParagraphNameContext ctx) {
    String name = ctx.getText().toUpperCase();
    getLocality(ctx.getStart())
        .ifPresent(locality -> groupContext.addParagraphDefinition(name, locality));
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitDataDescriptionEntryFormat1(DataDescriptionEntryFormat1Context ctx) {
    String name = getName(ctx.entryName());
    NodeType nodeType = getDataDescriptionNodeType(ctx);
    int level = getLevel(ctx.LEVEL_NUMBER());
    outlineTreeBuilder.addVariable(level, name, nodeType, ctx);
    return addTreeNode(
        VariableDefinitionNode.builder()
            .level(level)
            .levelLocality(getLevelLocality(ctx.LEVEL_NUMBER()))
            .locality(retrieveRangeLocality(ctx, positionMapping).orElse(null))
            .variableName(extractNameAndLocality(ctx.entryName()))
            .global(!ctx.dataGlobalClause().isEmpty())
            .picClauses(retrievePicTexts(ctx.dataPictureClause()))
            .valueClauses(retrieveValues(ctx.dataValueClause()))
            .usageClauses(retrieveUsageFormat(ctx.dataUsageClause()))
            .occursClauses(retrieveOccursValues(ctx.dataOccursClause()))
            .redefinesClauses(
                ctx.dataRedefinesClause().stream()
                    .map(DataRedefinesClauseContext::dataName)
                    .map(this::extractNameAndLocality)
                    .collect(toList()))
            .blankWhenZero(!ctx.dataBlankWhenZeroClause().isEmpty())
            .signClause(!ctx.dataSignClause().isEmpty())
            .build(),
        visitChildren(ctx));
  }

  @Override
  public List<Node> visitEnvironmentSwitchNameClause(EnvironmentSwitchNameClauseContext ctx) {
    Locality locality =
        getLocality(
                Optional.<ParserRuleContext>ofNullable(ctx.mnemonicName()).orElse(ctx).getStart())
            .orElse(null);
    String name = ofNullable(ctx.mnemonicName()).map(RuleContext::getText).orElse(FILLER_NAME);
    String systemName = ctx.environmentName().getText();
    outlineTreeBuilder.addNode(name, NodeType.MNEMONIC_NAME, ctx);
    return addTreeNode(
        VariableDefinitionNode.builder()
            .locality(locality)
            .variableName(new VariableNameAndLocality(name, locality))
            .systemName(systemName)
            .build(),
        visitChildren(ctx));
  }

  @Override
  public List<Node> visitDataDescriptionEntryFormat2(DataDescriptionEntryFormat2Context ctx) {
    String name = getName(ctx.entryName());
    outlineTreeBuilder.addVariable(LEVEL_66, name, NodeType.FIELD_66, ctx);
    Builder builder =
        VariableDefinitionNode.builder()
            .level(LEVEL_66)
            .levelLocality(getLevelLocality(ctx.LEVEL_NUMBER_66()))
            .variableName(extractNameAndLocality(ctx.entryName()))
            .locality(retrieveRangeLocality(ctx, positionMapping).orElse(null))
            .renamesClause(extractNameAndLocality(ctx.dataRenamesClause().dataName()));
    ofNullable(ctx.dataRenamesClause().thruDataName())
        .map(ThruDataNameContext::dataName)
        .map(this::extractNameAndLocality)
        .ifPresent(builder::renamesThruClause);
    return addTreeNode(builder.build(), visitChildren(ctx));
  }

  @Override
  public List<Node> visitDataDescriptionEntryFormat3(DataDescriptionEntryFormat3Context ctx) {
    String name = getName(ctx.entryName());
    outlineTreeBuilder.addVariable(LEVEL_88, name, NodeType.FIELD_88, ctx);
    return addTreeNode(
        VariableDefinitionNode.builder()
            .level(LEVEL_88)
            .levelLocality(getLevelLocality(ctx.LEVEL_NUMBER_88()))
            .variableName(extractNameAndLocality(ctx.entryName()))
            .locality(retrieveRangeLocality(ctx, positionMapping).orElse(null))
            .valueClauses(retrieveValues(ImmutableList.of(ctx.dataValueClause())))
            .build(),
        visitChildren(ctx));
  }

  @Override
  public List<Node> visitDataDescriptionEntryFormat1Level77(
      DataDescriptionEntryFormat1Level77Context ctx) {
    String name = getName(ctx.entryName());
    outlineTreeBuilder.addVariable(LEVEL_77, name, NodeType.FIELD, ctx);
    return addTreeNode(
        VariableDefinitionNode.builder()
            .level(LEVEL_77)
            .levelLocality(getLevelLocality(ctx.LEVEL_NUMBER_77()))
            .variableName(extractNameAndLocality(ctx.entryName()))
            .locality(retrieveRangeLocality(ctx, positionMapping).orElse(null))
            .global(!ctx.dataGlobalClause().isEmpty())
            .picClauses(retrievePicTexts(ctx.dataPictureClause()))
            .valueClauses(retrieveValues(ctx.dataValueClause()))
            .usageClauses(retrieveUsageFormat(ctx.dataUsageClause()))
            .occursClauses(retrieveOccursValues(ctx.dataOccursClause()))
            .redefinesClauses(
                ctx.dataRedefinesClause().stream()
                    .map(DataRedefinesClauseContext::dataName)
                    .map(this::extractNameAndLocality)
                    .collect(toList()))
            .blankWhenZero(!ctx.dataBlankWhenZeroClause().isEmpty())
            .signClause(!ctx.dataSignClause().isEmpty())
            .build(),
        visitChildren(ctx));
  }

  @Override
  public List<Node> visitParagraphNameUsage(ParagraphNameUsageContext ctx) {
    String name = ctx.getText().toUpperCase();
    getLocality(ctx.getStart()).ifPresent(l -> groupContext.addCandidateUsage(name, l));
    return visitChildren(ctx);
  }

  private List<DocumentSymbol> buildOutlineTree() {
    return outlineTreeBuilder.build(copybooks.getUsages());
  }

  @Override
  public List<Node> visitSetUpDownByStatement(SetUpDownByStatementContext ctx) {
    List<Locality> receivingFields = mapRulesToLocalities(ctx.receivingField());
    Locality sendingField = positionMapping.get(ctx.sendingField().getStart());
    String literal =
        ofNullable(ctx.sendingField().literal()).map(ParserRuleContext::getText).orElse(null);
    return addTreeNode(
        ctx,
        locality -> new SetUpDownByStatement(locality, receivingFields, sendingField, literal));
  }

  @Override
  public List<Node> visitSetToOnOff(SetToOnOffContext ctx) {
    return addTreeNode(
        ctx,
        locality -> new SetToOnOffStatement(locality, mapRulesToLocalities(ctx.receivingField())));
  }

  @Override
  public List<Node> visitSetToBoolean(SetToBooleanContext ctx) {
    return addTreeNode(
        ctx,
        locality ->
            new SetToBooleanStatement(locality, mapRulesToLocalities(ctx.receivingField())));
  }

  @Override
  public List<Node> visitQualifiedDataNameFormat1(QualifiedDataNameFormat1Context ctx) {
    return addVariableUsage(ctx.dataName(), ctx, createParentsList(ctx.inData(), ctx.inTable()));
  }

  @Override
  public List<Node> visitConditionNameReference(ConditionNameReferenceContext ctx) {
    return addVariableUsage(
        ctx.dataName(), ctx, createParentsList(ctx.inData(), ImmutableList.of()));
  }

  @Override
  public List<Node> visitIdms_map_name(Idms_map_nameContext ctx) {
    return addVariableUsage(ctx.dataName(), ctx);
  }

  @Override
  public List<Node> visitIdms_db_entity_name(Idms_db_entity_nameContext ctx) {
    return addVariableUsage(ctx.dataName(), ctx);
  }

  @Override
  public List<Node> visitIdms_procedure_name(Idms_procedure_nameContext ctx) {
    return addVariableUsage(ctx.dataName(), ctx);
  }

  @Override
  public List<Node> visitTableCall(TableCallContext ctx) {
    return addVariableUsage(ctx.dataName(), ctx);
  }

  private List<Node> addVariableUsage(
      ParserRuleContext nameContext, ParserRuleContext parentContext) {
    return addVariableUsage(nameContext, parentContext, ImmutableList.of());
  }

  private List<Node> addVariableUsage(
      ParserRuleContext nameContext,
      ParserRuleContext parentContext,
      List<VariableNameAndLocality> hierarchy) {
    String dataName = getName(nameContext);
    List<Node> result = new ArrayList<>();
    getLocality(nameContext.getStart())
        .ifPresent(
            locality -> {
              if (constants.contains(dataName)) constants.addUsage(dataName, locality.toLocation());
              else {
                result.add(new VariableUsageNode(dataName, locality, hierarchy));
              }
            });
    result.addAll(visitChildren(parentContext));
    return result;
  }

  private List<VariableNameAndLocality> createParentsList(
      List<InDataContext> inData, List<InTableContext> inTable) {
    return retrieveDataNames(inData, inTable)
        .map(
            it ->
                getLocality(it.getStart())
                    .map(loc -> new VariableNameAndLocality(it.getText().toUpperCase(), loc))
                    .orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private Stream<DataNameContext> retrieveDataNames(
      List<InDataContext> inData, List<InTableContext> inTable) {
    return Streams.concat(
        inData.stream().map(InDataContext::dataName),
        inTable.stream().map(InTableContext::tableCall).map(TableCallContext::dataName));
  }

  @Override
  public List<Node> visitCallStatement(CallStatementContext ctx) {
    if (ctx.literal() != null) {
      String subroutineName =
          PreprocessorStringUtils.trimQuotes(ctx.literal().getText()).toUpperCase();
      Optional<Locality> locality = getLocality(ctx.literal().getStart());
      locality.ifPresent(
          it -> {
            if (!subroutineService.getUri(subroutineName).isPresent()) {
              reportSubroutineNotDefined(subroutineName, it);
            }
          });
      locality
          .map(Locality::toLocation)
          .ifPresent(location -> subroutineUsages.put(subroutineName, location));
    }
    return visitChildren(ctx);
  }

  @Override
  public List<Node> visitGoToStatement(GoToStatementContext ctx) {
    GoToStatementSimpleContext goToStatementSimpleContext = ctx.goToStatementSimple();
    if (goToStatementSimpleContext == null) {
      return visitChildren(ctx);
    }
    List<String> targets =
        Collections.singletonList(
            goToStatementSimpleContext.procedureName().paragraphNameUsage().getText());
    return addTreeNode(ctx, locality -> new GoToNode(locality, targets));
  }

  @Override
  public List<Node> visitExitStatement(ExitStatementContext ctx) {
    return addTreeNode(ctx, ExitNode::new);
  }

  @Override
  public List<Node> visitGobackStatement(GobackStatementContext ctx) {
    return addTreeNode(ctx, GoBackNode::new);
  }

  @Override
  public List<Node> visitStopStatement(StopStatementContext ctx) {
    return addTreeNode(ctx, StopNode::new);
  }

  @Override
  public List<Node> visitEvaluateStatement(EvaluateStatementContext ctx) {
    return addTreeNode(ctx, EvaluateNode::new);
  }

  @Override
  public List<Node> visitEvaluateWhen(EvaluateWhenContext ctx) {
    return addTreeNode(ctx, EvaluateWhenNode::new);
  }

  @Override
  public List<Node> visitIfStatement(IfStatementContext ctx) {
    return addTreeNode(ctx, locality -> new IfNode(locality, ctx));
  }
  // NOTE: CobolVisitor is not managed by Guice DI, so can't use annotation here.
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
    List<Node> result = new ArrayList<>(aggregate.size() + nextResult.size());
    result.addAll(aggregate);
    result.addAll(nextResult);
    return result;
  }

  private void throwException(String wrongToken, @NonNull Locality locality, String message) {
    SyntaxError error =
        SyntaxError.syntaxError()
            .locality(locality)
            .suggestion(message + wrongToken)
            .severity(ErrorSeverity.WARNING)
            .build();

    LOG.debug("Syntax error by CobolVisitor#throwException: {}", error);
    if (!errors.contains(error)) {
      errors.add(error);
    }
  }

  private List<Locality> mapRulesToLocalities(List<? extends ParserRuleContext> rules) {
    return rules.stream()
        .map(ParserRuleContext::getStart)
        .map(this::getLocality)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private Optional<Locality> getLocality(Token childToken) {
    return ofNullable(positionMapping.get(childToken));
  }

  private void reportSubroutineNotDefined(String name, Locality locality) {
    SyntaxError error =
        SyntaxError.syntaxError()
            .suggestion(messageService.getMessage("CobolVisitor.subroutineNotFound", name))
            .severity(ErrorSeverity.INFO)
            .locality(getIntervalPosition(locality, locality))
            .build();
    LOG.debug("Syntax error by CobolVisitor#reportSubroutineNotDefined: {}", error);
    errors.add(error);
  }

  private void throwWarning(Token token) {
    String tokenText = token.getText().toUpperCase();
    if (MisspelledKeywordDistance.KEYWORDS.getSuggestions().contains(tokenText)) return;
    MisspelledKeywordDistance.calculateDistance(tokenText)
        .ifPresent(
            correctWord ->
                getLocality(token)
                    .ifPresent(locality -> reportMisspelledKeyword(correctWord, locality)));
  }

  private void reportMisspelledKeyword(String suggestion, Locality locality) {
    if (locality == null) return;
    SyntaxError error =
        SyntaxError.syntaxError()
            .suggestion(messageService.getMessage("CobolVisitor.misspelledWord", suggestion))
            .severity(ErrorSeverity.WARNING)
            .locality(locality)
            .build();
    LOG.debug("Syntax error by CobolVisitor#reportMisspelledKeyword: {}", error);
    errors.add(error);
  }

  private void areaAWarning(Token token) {
    // skip area A check for cics and sql block
    if (token.getText().startsWith("EXEC")) {
      return;
    }
    getLocality(token)
        .filter(it -> it.getRange().getStart().getCharacter() > AREA_A_FINISH)
        .ifPresent(
            it ->
                throwException(
                    token.getText(),
                    it,
                    messageService.getMessage("CobolVisitor.AreaAWarningMsg")));
  }

  private void areaBWarning(ParserRuleContext ctx) {
    areaBWarning(
        tokenStream.getTokens(ctx.getStart().getTokenIndex(), ctx.getStop().getTokenIndex()));
  }

  private void areaBWarning(@NonNull List<Token> tokenList) {
    tokenList.forEach(
        token ->
            getLocality(token)
                .ifPresent(
                    locality -> {
                      int charPosition = locality.getRange().getStart().getCharacter();
                      if (charPosition > 6 && charPosition < 11 && token.getChannel() != 1) {
                        throwException(
                            token.getText(),
                            locality,
                            messageService.getMessage("CobolVisitor.AreaBWarningMsg"));
                      }
                    }));
  }

  private Map<String, Collection<Location>> getSubroutineDefinition() {
    return subroutineUsages.keySet().stream()
        .map(name -> new ImmutablePair<>(name, subroutineService.getUri(name)))
        .filter(pair -> pair.getValue().isPresent())
        .collect(toMap(Pair::getKey, CobolVisitor::getSubroutineLocation));
  }

  private static Collection<Location> getSubroutineLocation(
      ImmutablePair<String, Optional<String>> subroutinePair) {
    return subroutinePair
        .getValue()
        .map(it -> new Location(it, new Range(new Position(), new Position())))
        .map(Collections::singletonList)
        .orElse(emptyList());
  }

  private List<Node> addTreeNode(ParserRuleContext ctx, Function<Locality, Node> nodeConstructor) {
    List<Node> children = visitChildren(ctx);
    return retrieveRangeLocality(ctx, positionMapping)
        .map(constructNode(nodeConstructor, children))
        .orElse(children);
  }

  private List<Node> addTreeNode(Node node, List<Node> children) {
    if (node.getLocality() == null) return children;
    children.forEach(node::addChild);
    return ImmutableList.of(node);
  }

  private NodeType getDataDescriptionNodeType(DataDescriptionEntryFormat1Context ctx) {
    // Data description with a pic clause is a FIELD
    if (!ctx.dataPictureClause().isEmpty()) {
      return NodeType.FIELD;
    }
    // Data description with a redefines clause is a REDEFINES
    if (!ctx.dataRedefinesClause().isEmpty()) {
      return NodeType.REDEFINES;
    }
    // Data description is a STRUCT in other cases
    return NodeType.STRUCT;
  }

  // TODO: Add check that name does not present in the predefined variables list (? - to check)
  private VariableNameAndLocality extractNameAndLocality(EntryNameContext context) {
    if (context == null || context.dataName() == null) return null;
    return extractNameAndLocality(context.dataName());
  }

  private VariableNameAndLocality extractNameAndLocality(DataNameContext context) {
    return extractNameAndLocality(context.cobolWord());
  }

  private VariableNameAndLocality extractNameAndLocality(CobolWordContext context) {
    return new VariableNameAndLocality(
        getName(context), retrieveRangeLocality(context, positionMapping).orElse(null));
  }

  private List<OccursClause> retrieveOccursValues(List<DataOccursClauseContext> contexts) {
    // TODO: Process OCCURS DEPENDING ON
    return contexts.stream()
        .map(
            context ->
                new OccursClause(
                    getInteger(context.integerLiteral()),
                    ofNullable(context.dataOccursTo())
                        .map(DataOccursToContext::integerLiteral)
                        .map(VisitorHelper::getInteger)
                        .orElse(null),
                    ofNullable(context.indexName()).orElseGet(ImmutableList::of).stream()
                        .map(IndexNameContext::cobolWord)
                        .map(this::extractNameAndLocality)
                        .collect(toList())))
        .collect(toList());
  }

  private List<ValueClause> retrieveValues(List<DataValueClauseContext> clauses) {
    return clauses.stream().map(this::retrieveValue).collect(toList());
  }

  private ValueClause retrieveValue(DataValueClauseContext context) {
    return new ValueClause(
        retrieveValueIntervals(context.dataValueClauseLiteral().dataValueInterval()),
        positionMapping.get(context.getStart()));
  }

  private Locality getLevelLocality(TerminalNode terminalNode) {
    return positionMapping.get(terminalNode.getSymbol());
  }
}
