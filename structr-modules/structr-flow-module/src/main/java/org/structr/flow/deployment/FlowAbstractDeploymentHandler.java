/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;

public abstract class FlowAbstractDeploymentHandler implements FlowDeploymentInterface {

	protected static final String[] classesToExport = {
			StructrTraits.FLOW_ACTION,
			StructrTraits.FLOW_AND,
			StructrTraits.FLOW_CALL,
			StructrTraits.FLOW_CONTAINER,
			StructrTraits.FLOW_DATA_SOURCE,
			StructrTraits.FLOW_DECISION,
			StructrTraits.FLOW_FOR_EACH,
			StructrTraits.FLOW_GET_PROPERTY,
			StructrTraits.FLOW_KEY_VALUE,
			StructrTraits.FLOW_NOT,
			StructrTraits.FLOW_NOT_NULL,
			StructrTraits.FLOW_OBJECT_DATA_SOURCE,
			StructrTraits.FLOW_OR,
			StructrTraits.FLOW_PARAMETER_INPUT,
			StructrTraits.FLOW_PARAMETER_DATA_SOURCE,
			StructrTraits.FLOW_RETURN,
			StructrTraits.FLOW_SCRIPT_CONDITION,
			StructrTraits.FLOW_STORE,
			StructrTraits.FLOW_AGGREGATE,
			StructrTraits.FLOW_CONSTANT,
			StructrTraits.FLOW_COLLECTION_DATA_SOURCE,
			StructrTraits.FLOW_EXCEPTION_HANDLER,
			StructrTraits.FLOW_TYPE_QUERY,
			StructrTraits.FLOW_IS_TRUE,
			StructrTraits.FLOW_CONTAINER_PACKAGE,
			StructrTraits.FLOW_LOG,
			StructrTraits.FLOW_FIRST,
			StructrTraits.FLOW_NOT_EMPTY,
			StructrTraits.FLOW_FILTER,
			StructrTraits.FLOW_COMPARISON,
			StructrTraits.FLOW_FORK,
			StructrTraits.FLOW_FORK_JOIN,
			StructrTraits.FLOW_SWITCH,
			StructrTraits.FLOW_SWITCH_CASE
	};

	protected static final String[] relsToExport = {
			StructrTraits.FLOW_CALL_CONTAINER,
			StructrTraits.FLOW_CALL_PARAMETER,
			StructrTraits.FLOW_CONDITION_CONDITION,
			StructrTraits.FLOW_CONTAINER_BASE_NODE,
			StructrTraits.FLOW_CONTAINER_FLOW_NODE,
			StructrTraits.FLOW_DATA_INPUT,
			StructrTraits.FLOW_DATA_INPUTS,
			StructrTraits.FLOW_DECISION_CONDITION,
			StructrTraits.FLOW_DECISION_FALSE,
			StructrTraits.FLOW_DECISION_TRUE,
			StructrTraits.FLOW_FOR_EACH_BODY,
			StructrTraits.FLOW_KEY_VALUE_OBJECT_INPUT,
			StructrTraits.FLOW_NAME_DATA_SOURCE,
			StructrTraits.FLOW_NODE_DATA_SOURCE,
			StructrTraits.FLOW_NODES,
			StructrTraits.FLOW_AGGREGATE_START_VALUE,
			StructrTraits.FLOW_SCRIPT_CONDITION_SOURCE,
			StructrTraits.FLOW_ACTIVE_CONTAINER_CONFIGURATION,
			StructrTraits.FLOW_EXCEPTION_HANDLER_NODES,
			StructrTraits.FLOW_CONTAINER_PACKAGE_FLOW,
			StructrTraits.FLOW_CONTAINER_PACKAGE_PACKAGE,
			StructrTraits.FLOW_CONDITION_BASE_NODE,
			StructrTraits.FLOW_FORK_BODY,
			StructrTraits.FLOW_SWITCH_CASES
	};

	protected static void cleanupFlows() throws FrameworkException {
		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			// Cleanup old flow data
			for (final String c : classesToExport) {

				for (final NodeInterface toDelete : app.nodeQuery(c).getAsList()) {

					app.delete(toDelete);
				}
			}

			for (final String c : relsToExport) {

				for (final RelationshipInterface toDelete : app.relationshipQuery(c).getAsList()) {

					app.delete(toDelete);
				}
			}

			for (final RelationshipInterface toDelete : app.relationshipQuery(StructrTraits.FLOW_CONTAINER_CONFIGURATION_FLOW).getAsList()) {

				app.delete(toDelete);
			}

			tx.success();
		}
	}

}
