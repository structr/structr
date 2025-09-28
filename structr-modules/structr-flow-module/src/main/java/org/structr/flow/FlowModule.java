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
import org.structr.core.function.Functions;
import org.structr.core.property.EndNode;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.flow.datasource.FlowContainerDataSource;
import org.structr.flow.impl.FlowFunction;
import org.structr.flow.impl.rels.*;
import org.structr.flow.traits.definitions.*;
import org.structr.module.StructrModule;

import java.nio.file.Path;
import java.util.Set;

public class FlowModule implements StructrModule {

	@Override
	public void onLoad() {

		DataSources.put(getName(), "flowDataSource", new FlowContainerDataSource());

		StructrTraits.registerTrait(new DOMNodeFLOWFlowContainer());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_FLOW_FLOW_CONTAINER, StructrTraits.DOM_NODE_FLOW_FLOW_CONTAINER);

		final TraitsInstance rootInstance = TraitsManager.getRootInstance();

		// register DOMNode -> FlowContainer relationship
		Traits.getTrait(StructrTraits.DOM_NODE).registerPropertyKey(new EndNode(rootInstance, "flow", "DOMNodeFLOWFlowContainer"));

		// relationships: traits
		StructrTraits.registerTrait(new FlowActiveContainerConfiguration());
		StructrTraits.registerTrait(new FlowAggregateStartValue());
		StructrTraits.registerTrait(new FlowCallContainer());
		StructrTraits.registerTrait(new FlowCallParameter());
		StructrTraits.registerTrait(new FlowConditionBaseNode());
		StructrTraits.registerTrait(new FlowConditionCondition());
		StructrTraits.registerTrait(new FlowContainerBaseNode());
		StructrTraits.registerTrait(new FlowContainerConfigurationFlow());
		StructrTraits.registerTrait(new FlowContainerConfigurationPrincipal());
		StructrTraits.registerTrait(new FlowContainerFlowNode());
		StructrTraits.registerTrait(new FlowContainerPackageFlow());
		StructrTraits.registerTrait(new FlowContainerPackagePackage());
		StructrTraits.registerTrait(new FlowDataInput());
		StructrTraits.registerTrait(new FlowDataInputs());
		StructrTraits.registerTrait(new FlowDecisionCondition());
		StructrTraits.registerTrait(new FlowDecisionFalse());
		StructrTraits.registerTrait(new FlowDecisionTrue());
		StructrTraits.registerTrait(new FlowExceptionHandlerNodes());
		StructrTraits.registerTrait(new FlowForEachBody());
		StructrTraits.registerTrait(new FlowForkBody());
		StructrTraits.registerTrait(new FlowKeyValueObjectInput());
		StructrTraits.registerTrait(new FlowNameDataSource());
		StructrTraits.registerTrait(new FlowNodeDataSource());
		StructrTraits.registerTrait(new FlowNodes());
		StructrTraits.registerTrait(new FlowScriptConditionSource());
		StructrTraits.registerTrait(new FlowSwitchCases());
		StructrTraits.registerTrait(new FlowValueInput());

		//relationships: types
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_ACTIVE_CONTAINER_CONFIGURATION,    StructrTraits.FLOW_ACTIVE_CONTAINER_CONFIGURATION);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_AGGREGATE_START_VALUE,             StructrTraits.FLOW_AGGREGATE_START_VALUE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CALL_CONTAINER,                    StructrTraits.FLOW_CALL_CONTAINER);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CALL_PARAMETER,                    StructrTraits.FLOW_CALL_PARAMETER);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONDITION_BASE_NODE,               StructrTraits.FLOW_CONDITION_BASE_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONDITION_CONDITION,               StructrTraits.FLOW_CONDITION_CONDITION);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_BASE_NODE,               StructrTraits.FLOW_CONTAINER_BASE_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_CONFIGURATION_FLOW,      StructrTraits.FLOW_CONTAINER_CONFIGURATION_FLOW);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_CONFIGURATION_PRINCIPAL, StructrTraits.FLOW_CONTAINER_CONFIGURATION_PRINCIPAL);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_FLOW_NODE,               StructrTraits.FLOW_CONTAINER_FLOW_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_PACKAGE_FLOW,            StructrTraits.FLOW_CONTAINER_PACKAGE_FLOW);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_CONTAINER_PACKAGE_PACKAGE,         StructrTraits.FLOW_CONTAINER_PACKAGE_PACKAGE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DATA_INPUT,                        StructrTraits.FLOW_DATA_INPUT);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DATA_INPUTS,                       StructrTraits.FLOW_DATA_INPUTS);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DECISION_CONDITION,                StructrTraits.FLOW_DECISION_CONDITION);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DECISION_FALSE,                    StructrTraits.FLOW_DECISION_FALSE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_DECISION_TRUE,                     StructrTraits.FLOW_DECISION_TRUE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_EXCEPTION_HANDLER_NODES,           StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_FOR_EACH_BODY,                     StructrTraits.FLOW_FOR_EACH_BODY);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_FORK_BODY,                         StructrTraits.FLOW_FORK_BODY);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_KEY_VALUE_OBJECT_INPUT,            StructrTraits.FLOW_KEY_VALUE_OBJECT_INPUT);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_NAME_DATA_SOURCE,                  StructrTraits.FLOW_NAME_DATA_SOURCE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_NODE_DATA_SOURCE,                  StructrTraits.FLOW_NODE_DATA_SOURCE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_NODES,                             StructrTraits.FLOW_NODES);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_SCRIPT_CONDITION_SOURCE,           StructrTraits.FLOW_SCRIPT_CONDITION_SOURCE);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_SWITCH_CASES,                      StructrTraits.FLOW_SWITCH_CASES);
		StructrTraits.registerRelationshipType(StructrTraits.FLOW_VALUE_INPUT,                       StructrTraits.FLOW_VALUE_INPUT);

		// nodes: traits
		StructrTraits.registerTrait(new FlowActionTraitDefinition());
		StructrTraits.registerTrait(new FlowAggregateTraitDefinition());
		StructrTraits.registerTrait(new FlowAndTraitDefinition());
		StructrTraits.registerTrait(new FlowBaseNodeTraitDefinition());
		StructrTraits.registerTrait(new FlowCallTraitDefinition());
		StructrTraits.registerTrait(new FlowCollectionDataSourceTraitDefinition());
		StructrTraits.registerTrait( new FlowComparisonTraitDefinition());
		StructrTraits.registerTrait(new FlowConditionTraitDefinition());
		StructrTraits.registerTrait(new FlowConstantTraitDefinition());
		StructrTraits.registerTrait(new FlowContainerTraitDefinition());
		StructrTraits.registerTrait(new FlowContainerConfigurationTraitDefinition());
		StructrTraits.registerTrait(new FlowContainerPackageTraitDefinition());
		StructrTraits.registerTrait(new FlowDecisionTraitDefinition());
		StructrTraits.registerTrait(new FlowDataSourceTraitDefinition());
		StructrTraits.registerTrait(new FlowExceptionHandlerTraitDefinition());
		StructrTraits.registerTrait(new FlowFilterTraitDefinition());
		StructrTraits.registerTrait(new FlowFirstTraitDefinition());
		StructrTraits.registerTrait(new FlowForEachTraitDefinition());
		StructrTraits.registerTrait(new FlowForkTraitDefinition());
		StructrTraits.registerTrait(new FlowForkJoinTraitDefinition());
		StructrTraits.registerTrait(new FlowGetPropertyTraitDefinition());
		StructrTraits.registerTrait(new FlowIsTrueTraitDefinition());
		StructrTraits.registerTrait(new FlowKeyValueTraitDefinition());
		StructrTraits.registerTrait(new FlowLogTraitDefinition());
		StructrTraits.registerTrait(new FlowLogicConditionTraitDefinition());
		StructrTraits.registerTrait(new FlowNodeTraitDefinition());
		StructrTraits.registerTrait(new FlowLogicConditionTraitDefinition());
		StructrTraits.registerTrait(new FlowNotTraitDefinition());
		StructrTraits.registerTrait(new FlowNotEmptyTraitDefinition());
		StructrTraits.registerTrait(new FlowNotNullTraitDefinition());
		StructrTraits.registerTrait(new FlowObjectDataSourceTraitDefinition());
		StructrTraits.registerTrait(new FlowOrTraitDefinition());
		StructrTraits.registerTrait(new FlowParameterDataSourceTraitDefinition());
		StructrTraits.registerTrait(new FlowParameterInputTraitDefinition());
		StructrTraits.registerTrait(new FlowReturnTraitDefinition());
		StructrTraits.registerTrait(new FlowScriptConditionTraitDefinition());
		StructrTraits.registerTrait(new FlowStoreTraitDefinition());
		StructrTraits.registerTrait(new FlowSwitchTraitDefinition());
		StructrTraits.registerTrait(new FlowSwitchCaseTraitDefinition());
		StructrTraits.registerTrait(new FlowTypeQueryTraitDefinition());

		// nodes: types
		StructrTraits.registerNodeType(StructrTraits.FLOW_ACTION,                  StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_ACTION);
		StructrTraits.registerNodeType(StructrTraits.FLOW_AGGREGATE,               StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_AGGREGATE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_AND,                     StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONDITION, StructrTraits.FLOW_LOGIC_CONDITION, StructrTraits.FLOW_AND);
		StructrTraits.registerNodeType(StructrTraits.FLOW_BASE_NODE,               StructrTraits.FLOW_BASE_NODE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_CALL,                    StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_CALL);
		StructrTraits.registerNodeType(StructrTraits.FLOW_COLLECTION_DATA_SOURCE,  StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_COLLECTION_DATA_SOURCE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_COMPARISON,              StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONDITION,   StructrTraits.FLOW_COMPARISON);
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONDITION,               StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_CONDITION);
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONSTANT,                StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_CONSTANT);
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONTAINER,               StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONTAINER);
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONTAINER_CONFIGURATION, StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONTAINER_CONFIGURATION);
		StructrTraits.registerNodeType(StructrTraits.FLOW_CONTAINER_PACKAGE,       StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONTAINER_PACKAGE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_DECISION,                StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DECISION);
		StructrTraits.registerNodeType(StructrTraits.FLOW_DATA_SOURCE,             StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_DATA_SOURCE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_EXCEPTION_HANDLER,       StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_EXCEPTION_HANDLER);
		StructrTraits.registerNodeType(StructrTraits.FLOW_FILTER,                  StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_FILTER);
		StructrTraits.registerNodeType(StructrTraits.FLOW_FIRST,                   StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_FIRST);
		StructrTraits.registerNodeType(StructrTraits.FLOW_FOR_EACH,                StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_FOR_EACH);
		StructrTraits.registerNodeType(StructrTraits.FLOW_FORK,                    StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_FORK);
		StructrTraits.registerNodeType(StructrTraits.FLOW_FORK_JOIN,               StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_FORK_JOIN);
		StructrTraits.registerNodeType(StructrTraits.FLOW_GET_PROPERTY,            StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_GET_PROPERTY);
		StructrTraits.registerNodeType(StructrTraits.FLOW_IS_TRUE,                 StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONDITION, StructrTraits.FLOW_IS_TRUE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_KEY_VALUE,               StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_KEY_VALUE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_LOG,                     StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_LOG);
		StructrTraits.registerNodeType(StructrTraits.FLOW_LOGIC_CONDITION,         StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_LOGIC_CONDITION);
		StructrTraits.registerNodeType(StructrTraits.FLOW_NODE,                    StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_NOT,                     StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONDITION, StructrTraits.FLOW_LOGIC_CONDITION, StructrTraits.FLOW_NOT);
		StructrTraits.registerNodeType(StructrTraits.FLOW_NOT_EMPTY,               StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONDITION, StructrTraits.FLOW_NOT_EMPTY);
		StructrTraits.registerNodeType(StructrTraits.FLOW_NOT_NULL,                StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONDITION, StructrTraits.FLOW_NOT_NULL);
		StructrTraits.registerNodeType(StructrTraits.FLOW_OBJECT_DATA_SOURCE,      StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_OBJECT_DATA_SOURCE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_OR,                      StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONDITION, StructrTraits.FLOW_LOGIC_CONDITION, StructrTraits.FLOW_OR);
		StructrTraits.registerNodeType(StructrTraits.FLOW_PARAMETER_DATA_SOURCE,   StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_PARAMETER_DATA_SOURCE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_PARAMETER_INPUT,         StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_PARAMETER_INPUT);
		StructrTraits.registerNodeType(StructrTraits.FLOW_RETURN,                  StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_RETURN);
		StructrTraits.registerNodeType(StructrTraits.FLOW_SCRIPT_CONDITION,        StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_CONDITION, StructrTraits.FLOW_SCRIPT_CONDITION);
		StructrTraits.registerNodeType(StructrTraits.FLOW_STORE,                   StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_STORE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_SWITCH,                  StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_SWITCH);
		StructrTraits.registerNodeType(StructrTraits.FLOW_SWITCH_CASE,             StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_SWITCH_CASE);
		StructrTraits.registerNodeType(StructrTraits.FLOW_TYPE_QUERY,              StructrTraits.FLOW_BASE_NODE, StructrTraits.FLOW_NODE, StructrTraits.FLOW_DATA_SOURCE, StructrTraits.FLOW_TYPE_QUERY);
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
