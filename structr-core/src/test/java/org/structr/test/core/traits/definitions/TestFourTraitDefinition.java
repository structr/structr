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
import org.structr.core.traits.definitions.AbstractTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class TestFourTraitDefinition extends AbstractTraitDefinition {

	public TestFourTraitDefinition() {
		super("TestFour");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> testOne             = new StartNode("testOne", "OneFourOneToOne");
		final Property<String[]>      stringArrayProperty = new ArrayProperty<>("stringArrayProperty", String.class).indexed();
		final Property<Boolean>       booleanProperty     = new BooleanProperty("booleanProperty").indexed();
		final Property<Double>        doubleProperty      = new DoubleProperty("doubleProperty").indexed();
		final Property<Integer>       integerProperty     = new IntProperty("integerProperty").indexed();
		final Property<Long>          longProperty        = new LongProperty("longProperty").indexed();
		final Property<Date>          dateProperty        = new DateProperty("dateProperty").indexed();
		final Property<String>        stringProperty      = new StringProperty("stringProperty").indexed();
		final Property<String>        enumProperty        = new EnumProperty("enumProperty", Set.of("Status1", "Status2", "Status3", "Status4")).indexed();
		final Property<Date[]>        dateArrayProperty   = new DateArrayProperty("dateArrayProperty").indexed();

		return Set.of(
			testOne,
			stringArrayProperty,
			booleanProperty,
			doubleProperty,
			integerProperty,
			longProperty,
			dateProperty,
			stringProperty,
			enumProperty,
			dateArrayProperty
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
