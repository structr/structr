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
package org.structr.test.rest.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Map;
import java.util.Set;

public class TestFiveTraitDefinition extends AbstractNodeTraitDefinition {

	public TestFiveTraitDefinition() {
		super("TestFive");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> manyToManyTestOnes = new EndNodes("manyToManyTestOnes", "FiveOneManyToMany", new PropertyNotion("id"));
		final Property<Iterable<NodeInterface>> oneToManyTestOnes  = new EndNodes("oneToManyTestOnes",  "FiveOneOneToMany", new PropertyNotion("id"));
		final Property<NodeInterface> oneToOneTestThree            = new EndNode("oneToOneTestThree",  "FiveThreeOneToOne");
		final Property<NodeInterface> manyToOneTestThree           = new StartNode("manyToOneTestThree", "ThreeFiveOneToMany");


		return Set.of(
			manyToManyTestOnes, oneToManyTestOnes, oneToOneTestThree, manyToOneTestThree
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			Set.of("name", "manyToManyTestOnes", "oneToManyTestOnes", "oneToOneTestThree", "manyToOneTestThree")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
