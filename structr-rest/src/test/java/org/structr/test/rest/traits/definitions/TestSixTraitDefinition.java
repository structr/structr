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
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;

import java.util.Map;
import java.util.Set;

public class TestSixTraitDefinition extends AbstractNodeTraitDefinition {

	public TestSixTraitDefinition() {
		super("TestSix");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> testSeven            = new StartNode("testSeven", "SevenSixOneToMany");
		final Property<String> testSevenName               = new EntityNotionProperty("testSevenName", "TestSix", "testSeven", "TestSeven", new PropertyNotion("name"));
		final Property<Iterable<NodeInterface>> testEights = new EndNodes("testEights", "SixEightManyToMany");
		final Property<Iterable<Integer>> testEightInts    = new CollectionNotionProperty("testEightInts", "TestSix", "testEights", "TestEight", new PropertyNotion("anInt"));
		final Property<Iterable<String>> testEightStrings  = new CollectionNotionProperty("testEightStrings", "TestSix", "testEights", "TestEight", new PropertyNotion("aString"));
		final Property<String> aString                     = new StringProperty("aString").indexed();
		final Property<Integer> anInt                      = new IntProperty("anInt").indexed();

		return newSet(
			testSeven, testSevenName, testEights, testEightInts, testEightStrings, aString, anInt
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(NodeInterfaceTraitDefinition.NAME_PROPERTY, "testSevenName", "testEightInts", "testEightStrings", "aString", "anInt")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
