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
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class TestSixTraitDefinition extends AbstractNodeTraitDefinition {

	public TestSixTraitDefinition() {
		super("TestSix");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> manyToManyTestOnes                  = new EndNodes("manyToManyTestOnes", "SixOneManyToMany");
		final Property<Iterable<NodeInterface>> oneToManyTestOnes                   = new EndNodes("oneToManyTestOnes",  "SixOneOneToMany");
		final Property<NodeInterface>           oneToOneTestThree                   = new EndNode("oneToOneTestThree",    "SixThreeOneToOne");
		final Property<Iterable<NodeInterface>> oneToManyTestThrees                 = new EndNodes("oneToManyTestThrees", "SixThreeOneToMany");
		final Property<Iterable<NodeInterface>> oneToManyTestThreesCascadeOut       = new EndNodes("oneToManyTestThreesCascadeOut",       "SixThreeOneToManyCascadeOutgoing");
		final Property<Iterable<NodeInterface>> oneToManyTestThreesCascadeIn        = new EndNodes("oneToManyTestThreesCascadeIn",        "SixThreeOneToManyCascadeIncoming");
		final Property<Iterable<NodeInterface>> oneToManyTestThreesCascadeBoth      = new EndNodes("oneToManyTestThreesCascadeBoth",      "SixThreeOneToManyCascadeBoth");
		final Property<Iterable<NodeInterface>> oneToManyTestNinesCascadeConstraint = new EndNodes("oneToManyTestNinesCascadeConstraint", "SixNineOneToManyCascadeConstraint");
		final Property<Integer>                 index                               = new IntProperty("index").indexed();
		final Property<Date>                    date                                = new DateProperty("date").indexed();

		return newSet(
			manyToManyTestOnes,
			oneToManyTestOnes,
			oneToOneTestThree,
			oneToManyTestThrees,
			oneToManyTestThreesCascadeOut,
			oneToManyTestThreesCascadeIn,
			oneToManyTestThreesCascadeBoth,
			oneToManyTestNinesCascadeConstraint,
			index,
			date
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
