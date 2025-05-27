/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.core.datasources.DataSources;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.core.property.EndNode;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.flow.datasource.FlowContainerDataSource;
import org.structr.flow.impl.FlowFunction;
import org.structr.flow.impl.rels.*;
import org.structr.flow.traits.definitions.*;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.nio.file.Path;
import java.util.Set;

public class FlowModule implements StructrModule {

	@Override
	public void onLoad() {

		DataSources.put(getName(), "flowDataSource", new FlowContainerDataSource());

		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_FLOW_FLOW_CONTAINER,           new DOMNodeFLOWFlowContainer());

		// register DOMNode -> FlowContainer relationship
		Traits.getTrait(StructrTraits.DOM_NODE).registerPropertyKey(new EndNode("flow", "DOMNodeFLOWFlowContainer"));

		StructrTraits.registerRelationshipType(StructrTraits.FLOW_ACTIVE_CONTAINER_CONFIGURATION,    new FlowActiveContainerConfiguration());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_AGGREGATE_START_VALUE,             new FlowAggregateStartValue());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CALL_CONTAINER,                    new FlowCallContainer());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CALL_PARAMETER,                    new FlowCallParameter());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONDITION_BASE_NODE,               new FlowConditionBaseNode());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONDITION_CONDITION,               new FlowConditionCondition());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_BASE_NODE,               new FlowContainerBaseNode());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_CONFIGURATION_FLOW,      new FlowContainerConfigurationFlow());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_CONFIGURATION_PRINCIPAL, new FlowContainerConfigurationPrincipal());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_FLOW_NODE,               new FlowContainerFlowNode());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_PACKAGE_FLOW,            new FlowContainerPackageFlow());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_PACKAGE_PACKAGE,         new FlowContainerPackagePackage());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DATA_INPUT,                        new FlowDataInput());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DATA_INPUTS,                       new FlowDataInputs());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DECISION_CONDITION,                new FlowDecisionCondition());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DECISION_FALSE,                    new FlowDecisionFalse());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DECISION_TRUE,                     new FlowDecisionTrue());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_EXCEPTION_HANDLER_NODES,           new FlowExceptionHandlerNodes());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_FOR_EACH_BODY,                     new FlowForEachBody());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_FORK_BODY,                         new FlowForkBody());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_KEY_VALUE_OBJECT_INPUT,            new FlowKeyValueObjectInput());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_NAME_DATA_SOURCE,                  new FlowNameDataSource());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_NODE_DATA_SOURCE,                  new FlowNodeDataSource());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_NODES,                             new FlowNodes());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_SCRIPT_CONDITION_SOURCE,           new FlowScriptConditionSource());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_SWITCH_CASES,                      new FlowSwitchCases());
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_VALUE_INPUT,                       new FlowValueInput());

		StructrTraits.registerNodeType(StructrTraits.FLOW_ACTION,                  new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowActionTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_AGGREGATE,               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowAggregateTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_AND,                     new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition(), new FlowLogicConditionTraitDefinition(), new FlowAndTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_BASE_NODE,               new FlowBaseNodeTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_CALL,                    new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowCallTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_COLLECTION_DATA_SOURCE,  new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowCollectionDataSourceTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_COMPARISON,              new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition(),  new FlowComparisonTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONDITION,               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowConditionTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONSTANT,                new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowConstantTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONTAINER,               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowContainerTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONTAINER_CONFIGURATION, new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowContainerConfigurationTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONTAINER_PACKAGE,       new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowContainerPackageTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_DECISION,                new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDecisionTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_DATA_SOURCE,             new FlowBaseNodeTraitDefinition(), new FlowDataSourceTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_EXCEPTION_HANDLER,       new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowExceptionHandlerTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_FILTER,                  new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowFilterTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_FIRST,                   new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowFirstTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_FOR_EACH,                new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowForEachTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_FORK,                    new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowForkTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_FORK_JOIN,               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowForkJoinTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_GET_PROPERTY,            new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowGetPropertyTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_IS_TRUE,                 new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition(), new FlowIsTrueTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_KEY_VALUE,               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowKeyValueTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_LOG,                     new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowLogTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_LOGIC_CONDITION,         new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowLogicConditionTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_NODE,                    new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_NOT,                     new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition(), new FlowLogicConditionTraitDefinition(), new FlowNotTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_NOT_EMPTY,               new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition(), new FlowNotEmptyTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_NOT_NULL,                new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition(), new FlowNotNullTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_OBJECT_DATA_SOURCE,      new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowObjectDataSourceTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_OR,                      new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition(), new FlowLogicConditionTraitDefinition(), new FlowOrTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_PARAMETER_DATA_SOURCE,   new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowParameterDataSourceTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_PARAMETER_INPUT,         new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowParameterInputTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_RETURN,                  new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowReturnTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_SCRIPT_CONDITION,        new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowConditionTraitDefinition(), new FlowScriptConditionTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_STORE,                   new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowStoreTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_SWITCH,                  new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowSwitchTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_SWITCH_CASE,             new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowSwitchCaseTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FLOW_TYPE_QUERY,              new FlowBaseNodeTraitDefinition(), new FlowNodeTraitDefinition(), new FlowDataSourceTraitDefinition(), new FlowTypeQueryTraitDefinition());
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
		return Set.of("ui");
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
