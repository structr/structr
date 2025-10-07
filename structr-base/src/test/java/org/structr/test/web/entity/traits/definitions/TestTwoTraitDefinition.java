/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.entity.traits.definitions;

import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Map;
import java.util.Set;

public class TestTwoTraitDefinition extends AbstractNodeTraitDefinition {

	public TestTwoTraitDefinition() {
		super("TestTwo");
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> testFives = new EndNodes(traitsInstance, "testFives", "TwoFiveOneToMany");

		return newSet(
			testFives
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
