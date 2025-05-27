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
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.test.rest.common.TestEnum;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class TestThreeTraitDefinition extends AbstractNodeTraitDefinition {

	public TestThreeTraitDefinition() {
		super("TestThree");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String>        stringProperty          = new StringProperty("stringProperty").fulltextIndexed();
		final Property<String[]>      stringArrayProperty     = new ArrayProperty<>("stringArrayProperty", String.class).indexedWhenEmpty();
		final Property<Boolean>       booleanProperty         = new BooleanProperty("booleanProperty").indexed();
		final Property<Boolean[]>     booleanArrayProperty    = new BooleanArrayProperty("booleanArrayProperty").indexedWhenEmpty();
		final Property<Double>        doubleProperty          = new DoubleProperty("doubleProperty").indexed().indexedWhenEmpty();
		final Property<Double[]>      doubleArrayProperty     = new ArrayProperty<>("doubleArrayProperty", Double.class).indexedWhenEmpty();
		final Property<Integer>       integerProperty         = new IntProperty("integerProperty").indexed().indexedWhenEmpty();
		final Property<Integer[]>     integerArrayProperty    = new ArrayProperty("integerArrayProperty", Integer.class).indexedWhenEmpty();
		final Property<Long>          longProperty            = new LongProperty("longProperty").indexed().indexedWhenEmpty();
		final Property<Long[]>        longArrayProperty       = new ArrayProperty("longArrayProperty", Long.class).indexedWhenEmpty();
		final Property<Date>          dateProperty            = new ISO8601DateProperty("dateProperty").indexed().indexedWhenEmpty();
		final Property<Date[]>        dateArrayProperty       = new DateArrayProperty("dateArrayProperty").indexed().indexedWhenEmpty();
		final Property<String>        enumProperty            = new EnumProperty("enumProperty", TestEnum.class).indexed();
		final Property<Boolean>       constantBooleanProperty = new ConstantBooleanProperty("constantBooleanProperty", true);
		final Property<Byte[]>        byteArrayProperty       = new ByteArrayProperty("byteArrayProperty").indexed().indexedWhenEmpty();
		final Property<NodeInterface> oneToOneTestFive        = new StartNode("oneToOneTestFive",  "FiveThreeOneToOne");
		final Property<ZonedDateTime> zonedDateTimeProperty   = new ZonedDateTimeProperty("zonedDateTime").indexed().indexedWhenEmpty();
		final Property<String[]>      testEnumArrayProperty   = new EnumArrayProperty("enumArrayProperty", TestEnum.class).indexed().indexedWhenEmpty();

		return newSet(
			stringProperty,
			stringArrayProperty,
			booleanProperty,
			booleanArrayProperty,
			doubleProperty,
			doubleArrayProperty,
			integerProperty,
			integerArrayProperty,
			longProperty,
			longArrayProperty,
			dateProperty,
			dateArrayProperty,
			enumProperty,
			constantBooleanProperty,
			byteArrayProperty,
			oneToOneTestFive,
			zonedDateTimeProperty,
			testEnumArrayProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"name", "stringProperty", "stringArrayProperty", "booleanProperty", "booleanArrayProperty", "doubleProperty", "doubleArrayProperty",
				"integerProperty", "integerArrayProperty", "longProperty", "longArrayProperty", "dateProperty", "dateArrayProperty", "zonedDateTime",
				"enumProperty", "constantBooleanProperty", "byteArrayProperty", "enumArrayProperty"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
