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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ValuesFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_VALUES = "Usage: ${values(entity, viewName)}. Example: ${values(this, \"ui\")}";

	@Override
	public String getName() {
		return "values";
	}

	@Override
	public String getSignature() {
		return "entity, viewName";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		//if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof GraphObject) {
		if (sources.length >= 2 && sources[0] instanceof GraphObject) {

			final Set<Object> values = new LinkedHashSet<>();
			final GraphObject source = (GraphObject) sources[0];

			for (final PropertyKey key : source.getPropertyKeys(sources[1].toString())) {
				values.add(source.getProperty(key));
			}

			return new LinkedList<>(values);

		//} else if (arrayHasMinLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof GraphObjectMap) {
		} else if (sources.length >= 1) {

			if (sources[0] instanceof GraphObjectMap) {

				return new LinkedList<>(((GraphObjectMap)sources[0]).values());

			} else if (sources[0] instanceof Map) {

				return new LinkedList<>(((Map)sources[0]).values());
			}

		}

		logParameterError(caller, sources, ctx.isJavaScriptContext());
		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_VALUES;
	}

	@Override
	public String shortDescription() {
		return "Returns the property values of the given entity";
	}

}
