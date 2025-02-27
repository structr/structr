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
package org.structr.flow.deployment;

public abstract class FlowAbstractDeploymentHandler implements FlowDeploymentInterface {

	protected static final String[] classesToExport = {
			"FlowAction",
			"FlowAnd",
			"FlowCall",
			"FlowContainer",
			"FlowDataSource",
			"FlowDecision",
			"FlowForEach",
			"FlowGetProperty",
			"FlowKeyValue",
			"FlowNot",
			"FlowNotNull",
			"FlowObjectDataSource",
			"FlowOr",
			"FlowParameterInput",
			"FlowParameterDataSource",
			"FlowReturn",
			"FlowScriptCondition",
			"FlowStore",
			"FlowAggregate",
			"FlowConstant",
			"FlowCollectionDataSource",
			"FlowExceptionHandler",
			"FlowTypeQuery",
			"FlowIsTrue",
			"FlowContainerPackage",
			"FlowLog",
			"FlowFirst",
			"FlowNotEmpty",
			"FlowFilter",
			"FlowComparison",
			"FlowFork",
			"FlowForkJoin",
			"FlowSwitch",
			"FlowSwitchCas",
	};

	protected static final String[] relsToExport = {
			"FlowCallContainer",
			"FlowCallParameter",
			"FlowConditionCondition",
			"FlowContainerBaseNode",
			"FlowContainerFlowNode",
			"FlowDataInput",
			"FlowDataInputs",
			"FlowDecisionCondition",
			"FlowDecisionFalse",
			"FlowDecisionTrue",
			"FlowForEachBody",
			"FlowKeyValueObjectInput",
			"FlowNameDataSource",
			"FlowNodeDataSource",
			"FlowNodes",
			"FlowAggregateStartValue",
			"FlowScriptConditionSource",
			"FlowActiveContainerConfiguration",
			"FlowExceptionHandlerNodes",
			"FlowContainerPackageFlow",
			"FlowContainerPackagePackage",
			"FlowConditionBaseNode",
			"FlowForkBody",
			"FlowSwitchCase",
	};

}
