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
package org.structr.test.traits.definitions;

import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.test.entity.CsvTestEnum;
import org.structr.test.entity.CsvTestOne;
import org.structr.test.traits.wrappers.CsvTestOneTraitWrapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * A simple entity for the most basic tests.
 *
 * The isValid method does always return true for testing purposes only.
 */
public class CsvTestOneTraitDefinition extends AbstractNodeTraitDefinition {

	public CsvTestOneTraitDefinition() {
		super("CsvTestOne");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			CsvTestOne.class, (traits, node) -> new CsvTestOneTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String[]>      stringArrayProperty = new ArrayProperty<>("stringArrayProperty", String.class).indexed();
		final Property<Boolean>       booleanProperty     = new BooleanProperty("booleanProperty").indexed();
		final Property<Double>        doubleProperty      = new DoubleProperty("doubleProperty").indexed();
		final Property<Integer>       integerProperty     = new IntProperty("integerProperty").indexed();
		final Property<Long>          longProperty        = new LongProperty("longProperty").indexed();
		final Property<Date>          dateProperty        = new DateProperty("dateProperty").indexed();
		final Property<String>        stringProperty      = new StringProperty("stringProperty").indexed();
		final Property<String>        enumProperty        = new EnumProperty("enumProperty", CsvTestEnum.class).indexed();
		final Property<Integer>       index               = new IntProperty("index");
		final Property<Integer[]>     intArrayProperty    = new ArrayProperty<>("intArrayProperty", Integer.class).indexed();

		return newSet(
			stringArrayProperty,
			booleanProperty,
			doubleProperty,
			integerProperty,
			longProperty,
			dateProperty,
			stringProperty,
			enumProperty,
			index,
			intArrayProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			"csv",
			newSet(
				"name", "index", "type", "stringArrayProperty", "enumProperty"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
