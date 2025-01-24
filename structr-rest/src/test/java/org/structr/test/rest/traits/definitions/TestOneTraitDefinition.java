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
import org.structr.core.property.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class TestOneTraitDefinition extends AbstractNodeTraitDefinition {

	/*

	public static final View defaultView = new View(TestOne.class, PropertyView.Public,
		name, anInt, aLong, aDate
	);

	@Export
	public RestMethodResult test01(final SecurityContext securityContext, final Map<String, Object> params) {
		return null;
	}

	@Export
	public void test02(final SecurityContext securityContext, final Map<String, Object> params) {
	}

	@Export
	public RestMethodResult test03(final SecurityContext securityContext) {
		return null;
	}

	@Export
	public void test04(final SecurityContext securityContext) {
	}
	*/

	fixme: methods!

	public TestOneTraitDefinition() {
		super("TestOne");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Integer> anInt = new IntProperty("anInt").indexed();
		final Property<Long> aLong    = new LongProperty("aLong").indexed();
		final Property<Date> aDate    = new ISO8601DateProperty("aDate").indexed();

		return newSet(
			anInt, aLong, aDate
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
				"name", "anInt", "aLong", "aDate"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
