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

import org.structr.core.entity.Relation;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.impl.FlowAnd;
import org.structr.flow.impl.FlowLogicCondition;
import org.structr.flow.traits.operations.LogicConditionOperations;

import java.util.Map;

public class FlowAndTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowAndTraitDefinition() {
		super("FlowAnd");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			LogicConditionOperations.class,
			new LogicConditionOperations() {

				@Override
				public Boolean combine(final FlowLogicCondition condition, final Boolean result, final Boolean value) {

					if (result == null) {
						return value;
					}

					return result && value;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowAnd.class, (traits, node) -> new FlowAnd(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
