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

import org.structr.flow.impl.*;
import org.structr.flow.impl.rels.*;

public abstract class FlowAbstractDeploymentHandler implements FlowDeploymentInterface {

	protected static final Class[] classesToExport = {
			FlowAction.class,
			FlowAnd.class,
			FlowCall.class,
			FlowContainer.class,
			FlowDataSource.class,
			FlowDecision.class,
			FlowForEach.class,
			FlowGetProperty.class,
			FlowKeyValue.class,
			FlowNot.class,
			FlowNotNull.class,
			FlowObjectDataSource.class,
			FlowOr.class,
			FlowParameterInput.class,
			FlowParameterDataSource.class,
			FlowReturn.class,
			FlowScriptCondition.class,
			FlowStore.class,
			FlowAggregate.class,
			FlowConstant.class,
			FlowCollectionDataSource.class,
			FlowExceptionHandler.class,
			FlowTypeQuery.class,
			FlowIsTrue.class,
			FlowContainerPackage.class,
			FlowLog.class,
			FlowFirst.class,
			FlowNotEmpty.class,
			FlowFilter.class,
			FlowComparison.class,
			FlowFork.class,
			FlowForkJoin.class,
			FlowSwitch.class,
			FlowSwitchCase.class
	};

	protected static final Class[] relsToExport = {
			FlowCallContainer.class,
			FlowCallParameter.class,
			FlowConditionCondition.class,
			FlowContainerBaseNode.class,
			FlowContainerFlowNode.class,
			FlowDataInput.class,
			FlowDataInputs.class,
			FlowDecisionCondition.class,
			FlowDecisionFalse.class,
			FlowDecisionTrue.class,
			FlowForEachBody.class,
			FlowKeyValueObjectInput.class,
			FlowNameDataSource.class,
			FlowNodeDataSource.class,
			FlowNodes.class,
			FlowAggregateStartValue.class,
			FlowScriptConditionSource.class,
			FlowActiveContainerConfiguration.class,
			FlowExceptionHandlerNodes.class,
			FlowContainerPackageFlow.class,
			FlowContainerPackagePackage.class,
			FlowConditionBaseNode.class,
			FlowForkBody.class,
			FlowSwitchCases.class
	};

}
