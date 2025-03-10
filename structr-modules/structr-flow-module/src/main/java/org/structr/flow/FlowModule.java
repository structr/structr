/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.core.datasources.DataSources;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.core.traits.StructrTraits;
import org.structr.flow.datasource.FlowContainerDataSource;
import org.structr.flow.impl.FlowFunction;
import org.structr.flow.impl.rels.*;
import org.structr.flow.traits.definitions.*;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.nio.file.Path;
import java.util.Set;

/**
 *
 */
public class FlowModule implements StructrModule {

	private static final Logger logger = LoggerFactory.getLogger(FlowModule.class.getName());

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		final boolean licensed = licenseManager == null || licenseManager.isModuleLicensed(getName());

		DataSources.put(licensed, getName(), "flowDataSource", new FlowContainerDataSource());

		if (licensed) {

			StructrTraits.registerRelationshipType("DOMNodeFLOWFlowContainer",            new DOMNodeFLOWFlowContainer());
			StructrTraits.registerRelationshipType("FlowActiveContainerConfiguration",    new FlowActiveContainerConfiguration());
			StructrTraits.registerRelationshipType("FlowAggregateStartValue",             new FlowAggregateStartValue());
			StructrTraits.registerRelationshipType("FlowCallContainer",                   new FlowCallContainer());
			StructrTraits.registerRelationshipType("FlowCallParameter",                   new FlowCallParameter());
			StructrTraits.registerRelationshipType("FlowConditionBaseNode",               new FlowConditionBaseNode());
			StructrTraits.registerRelationshipType("FlowConditionCondition",              new FlowConditionCondition());
			StructrTraits.registerRelationshipType("FlowContainerBaseNode",               new FlowContainerBaseNode());
			StructrTraits.registerRelationshipType("FlowContainerConfigurationFlow",      new FlowContainerConfigurationFlow());
			StructrTraits.registerRelationshipType("FlowContainerConfigurationPrincipal", new FlowContainerConfigurationPrincipal());
			StructrTraits.registerRelationshipType("FlowContainerFlowNode",               new FlowContainerFlowNode());
			StructrTraits.registerRelationshipType("FlowContainerPackageFlow",            new FlowContainerPackageFlow());
			StructrTraits.registerRelationshipType("FlowContainerPackagePackage",         new FlowContainerPackagePackage());
			StructrTraits.registerRelationshipType("FlowDataInput",                       new FlowDataInput());
			StructrTraits.registerRelationshipType("FlowDataInputs",                      new FlowDataInputs());
			StructrTraits.registerRelationshipType("FlowDecisionCondition",               new FlowDecisionCondition());
			StructrTraits.registerRelationshipType("FlowDecisionFalse",                   new FlowDecisionFalse());
			StructrTraits.registerRelationshipType("FlowDecisionTrue",                    new FlowDecisionTrue());
			StructrTraits.registerRelationshipType("FlowExceptionHandlerNodes",           new FlowExceptionHandlerNodes());
			StructrTraits.registerRelationshipType("FlowForEachBody",                     new FlowForEachBody());
			StructrTraits.registerRelationshipType("FlowForkBody",                        new FlowForkBody());
			StructrTraits.registerRelationshipType("FlowKeyValueObjectInput",             new FlowKeyValueObjectInput());
			StructrTraits.registerRelationshipType("FlowNameDataSource",                  new FlowNameDataSource());
			StructrTraits.registerRelationshipType("FlowNodeDataSource",                  new FlowNodeDataSource());
			StructrTraits.registerRelationshipType("FlowNodes",                           new FlowNodes());
			StructrTraits.registerRelationshipType("FlowScriptConditionSource",           new FlowScriptConditionSource());
			StructrTraits.registerRelationshipType("FlowSwitchCases",                     new FlowSwitchCases());

			StructrTraits.registerNodeType("FlowAction",                 new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowActionTraitDefinition());
			StructrTraits.registerNodeType("FlowAggregate",              new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowAggregateTraitDefinition());
			StructrTraits.registerNodeType("FlowAnd",                    new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowAndTraitDefinition());
			StructrTraits.registerNodeType("FlowBaseNode",               new FlowBaseNodeTraitDefinition());
			StructrTraits.registerNodeType("FlowCall",                   new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowCallTraitDefinition());
			StructrTraits.registerNodeType("FlowCollectionDataSource",   new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowCollectionDataSourceTraitDefinition());
			StructrTraits.registerNodeType("FlowComparison",             new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowComparisonTraitDefinition());
			StructrTraits.registerNodeType("FlowCondition",              new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition());
			StructrTraits.registerNodeType("FlowConstant",               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConstantTraitDefinition());
			StructrTraits.registerNodeType("FlowContainer",              new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowContainerTraitDefinition());
			StructrTraits.registerNodeType("FlowContainerConfiguration", new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowContainerConfigurationTraitDefinition());
			StructrTraits.registerNodeType("FlowContainerPackage",       new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowContainerPackageTraitDefinition());
			StructrTraits.registerNodeType("FlowDecision",               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDecisionTraitDefinition());
			StructrTraits.registerNodeType("FlowDataSource",             new FlowBaseNodeTraitDefinition(), new FlowDataSourceTraitDefinition());
			StructrTraits.registerNodeType("FlowExceptionHandler",       new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowExceptionHandlerTraitDefinition());
			StructrTraits.registerNodeType("FlowFilter",                 new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowFilterTraitDefinition());
			StructrTraits.registerNodeType("FlowFirst",                  new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowFirstTraitDefinition());
			StructrTraits.registerNodeType("FlowForEach",                new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowForEachTraitDefinition());
			StructrTraits.registerNodeType("FlowFork",                   new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowForkTraitDefinition());
			StructrTraits.registerNodeType("FlowForkJoin",               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowForkJoinTraitDefinition());
			StructrTraits.registerNodeType("FlowGetProperty",            new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowGetPropertyTraitDefinition());
			StructrTraits.registerNodeType("FlowIsTrue",                 new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowIsTrueTraitDefinition());
			StructrTraits.registerNodeType("FlowKeyValue",               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowKeyValueTraitDefinition());
			StructrTraits.registerNodeType("FlowLog",                    new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowLogTraitDefinition());
			StructrTraits.registerNodeType("FlowLogicConditions",        new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowLogicConditionTraitDefinition());
			StructrTraits.registerNodeType("FlowNode",                   new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition());
			StructrTraits.registerNodeType("FlowNot",                    new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowNotTraitDefinition());
			StructrTraits.registerNodeType("FlowNotEmpty",               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowNotEmptyTraitDefinition());
			StructrTraits.registerNodeType("FlowNotNull",                new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowNotNullTraitDefinition());
			StructrTraits.registerNodeType("FlowObjectDataSource",       new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowObjectDataSourceTraitDefinition());
			StructrTraits.registerNodeType("FlowOr",                     new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowOrTraitDefinition());
			StructrTraits.registerNodeType("FlowParameterDataSource",    new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowParameterDataSourceTraitDefinition());
			StructrTraits.registerNodeType("FlowParameterInput",         new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowParameterInputTraitDefinition());
			StructrTraits.registerNodeType("FlowReturn",                 new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowReturnTraitDefinition());
			StructrTraits.registerNodeType("FlowScriptCondition",        new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowScriptConditionTraitDefinition());
			StructrTraits.registerNodeType("FlowStore",                  new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowStoreTraitDefinition());
			StructrTraits.registerNodeType("FlowSwitch",                 new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowSwitchTraitDefinition());
			StructrTraits.registerNodeType("FlowSwitchCase",             new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowSwitchCaseTraitDefinition());
			StructrTraits.registerNodeType("FlowTypeQuery",              new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowTypeQueryTraitDefinition());
		}
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new FlowFunction());
	}

	@Override
	public String getName() {
		return "flows";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}

	@Override
	public boolean hasDeploymentData () {
		return true;
	}

	@Override
	public void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {
		FlowDeploymentHandler.exportDeploymentData(target, gson);
	}

	@Override
	public void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {
		FlowDeploymentHandler.importDeploymentData(source, gson);
	}
}
