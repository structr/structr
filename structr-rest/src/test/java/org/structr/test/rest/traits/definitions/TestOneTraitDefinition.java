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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.schema.action.EvaluationHints;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TestOneTraitDefinition extends AbstractNodeTraitDefinition {

	public TestOneTraitDefinition() {
		super("TestOne");
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		final Set<AbstractMethod> methods = new LinkedHashSet<>();

		methods.add(new JavaMethod("test01", false, false) {

			@Override
			public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
				return null;
			}
		});

		methods.add(new JavaMethod("test02", false, false) {

			@Override
			public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
				return null;
			}
		});

		methods.add(new JavaMethod("test03", false, false) {

			@Override
			public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
				return null;
			}
		});

		methods.add(new JavaMethod("test04", false, false) {

			@Override
			public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
				return null;
			}
		});

		return methods;
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
