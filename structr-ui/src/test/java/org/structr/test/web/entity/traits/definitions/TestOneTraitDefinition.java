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
package org.structr.test.web.entity.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;

public class TestOneTraitDefinition extends AbstractNodeTraitDefinition {

	public TestOneTraitDefinition() {
		super("TestOne");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Integer>       anInt              = new IntProperty("anInt").indexed().indexedWhenEmpty();
		final Property<Long>          aLong              = new LongProperty("aLong").indexed().indexedWhenEmpty();
		final Property<Double>        aDouble            = new DoubleProperty("aDouble").indexed().indexedWhenEmpty();
		final Property<Date>          aDate              = new ISO8601DateProperty("aDate").indexed().indexedWhenEmpty();
		final Property<String>        aString            = new StringProperty("aString").indexed().indexedWhenEmpty();
		final Property<String>        htmlString         = new StringProperty("htmlString");


		return newSet(
			anInt,
			aLong,
			aDouble,
			aDate,
			aString,
			htmlString
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
				NodeInterfaceTraitDefinition.NAME_PROPERTY, "anInt", "aDouble", "aLong", "aDate", GraphObjectTraitDefinition.CREATED_DATE_PROPERTY, "aString", "htmlString"
			),

			PropertyView.Ui,
			newSet(
				NodeInterfaceTraitDefinition.NAME_PROPERTY, "anInt", "aDouble", "aLong", "aDate", GraphObjectTraitDefinition.CREATED_DATE_PROPERTY, "aString", "htmlString"
			)
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
