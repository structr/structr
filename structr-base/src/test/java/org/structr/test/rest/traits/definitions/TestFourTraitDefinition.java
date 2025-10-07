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

import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Set;

public class TestFourTraitDefinition extends AbstractNodeTraitDefinition {

	public TestFourTraitDefinition() {
		super("TestFour");
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> manyToManyTestOnes = new EndNodes(traitsInstance, "manyToManyTestOnes", "FourOneManyToMany");
		final Property<Iterable<NodeInterface>> oneToManyTestOnes  = new EndNodes(traitsInstance, "oneToManyTestOnes",  "FourOneOneToMany");
		final Property<NodeInterface> oneToOneTestThree            = new EndNode(traitsInstance, "oneToOneTestThree",  "FourThreeOneToOne");
		final Property<NodeInterface> manyToOneTestThree           = new StartNode(traitsInstance, "manyToOneTestThree", "ThreeFourOneToMany");

		return newSet(
			manyToManyTestOnes, oneToManyTestOnes, oneToOneTestThree, manyToOneTestThree
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
