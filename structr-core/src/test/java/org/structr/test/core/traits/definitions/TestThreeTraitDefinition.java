/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.core.traits.definitions;

import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class TestThreeTraitDefinition extends AbstractNodeTraitDefinition {

	public TestThreeTraitDefinition() {
		super("TestThree");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final String TEST_THREE_CUSTOM_DATE_FORMAT = "dd.MM.yyyy";

		final Property<NodeInterface> testOne          = new StartNode("testOne",         "OneThreeOneToOne");
		final Property<NodeInterface> oneToOneTestSix  = new StartNode("oneToOneTestSix", "SixThreeOneToOne");
		final Property<NodeInterface> oneToManyTestSix = new StartNode("oneToManyTestSix", "SixThreeOneToMany");
		final Property<Date> aDateWithFormat           = new ISO8601DateProperty("aDateWithFormat").format(TEST_THREE_CUSTOM_DATE_FORMAT).indexed().indexedWhenEmpty();

		return newSet(
			testOne,
			oneToOneTestSix,
			oneToManyTestSix,
			aDateWithFormat
		);
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
