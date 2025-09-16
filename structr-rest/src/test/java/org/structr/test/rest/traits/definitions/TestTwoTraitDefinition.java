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
package org.structr.test.rest.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class TestTwoTraitDefinition extends AbstractNodeTraitDefinition {

	public TestTwoTraitDefinition() {
		super("TestTwo");
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Integer>       anInt    = new IntProperty("anInt").indexed().indexedWhenEmpty();
		final Property<Long>          aLong    = new LongProperty("aLong").indexed().indexedWhenEmpty();
		final Property<Date>          aDate    = new ISO8601DateProperty("aDate").indexed();

		final Property<Iterable<NodeInterface>> testOnes = new EndNodes(traitsInstance, "test_ones", "TwoOneOneToMany");
		final Property<Iterable<NodeInterface>> testOnesAlt = new EndNodes(traitsInstance, "testOnes", "TwoOneOneToMany");

		return newSet(
			anInt, aLong, aDate, testOnes, testOnesAlt
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(NodeInterfaceTraitDefinition.NAME_PROPERTY, "anInt", "aLong", "aDate", "test_ones", "testOnes")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
