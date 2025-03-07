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
package org.structr.flow.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.flow.impl.FlowScriptCondition;

import java.util.Map;
import java.util.Set;

public class FlowScriptConditionTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowScriptConditionTraitDefinition() {
		super("FlowScriptCondition");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowScriptCondition.class, (traits, node) -> new FlowScriptCondition(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> scriptSource         = new StartNode("scriptSource", "FlowScriptConditionSource");
		final Property<NodeInterface> dataSource           = new StartNode("dataSource", "FlowDataInput");
		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes("dataTarget", "FlowDataInput");
		final Property<NodeInterface> exceptionHandler     = new EndNode("exceptionHandler", "FlowExceptionHandlerNodes");
		final Property<String> script                      = new StringProperty("script");

		return newSet(
			scriptSource,
			dataSource,
			dataTarget,
			exceptionHandler,
			script
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"script", "scriptSource", "dataSource", "dataTarget", "exceptionHandler"
			),
			PropertyView.Ui,
			newSet(
				"script", "scriptSource", "dataSource", "dataTarget", "exceptionHandler"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
