/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ExtractFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_EXTRACT = "Usage: ${extract(list, propertyName)}. Example: ${extract(this.children, \"amount\")}";

	@Override
	public String getName() {
		return "extract()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			// no property key given, maybe we should extract a list of lists?
			if (sources[0] instanceof Collection) {

				final List extraction = new LinkedList();

				for (final Object obj : (Collection)sources[0]) {

					if (obj instanceof Collection) {

						extraction.addAll((Collection)obj);
					}
				}

				return extraction;
			}

		} else if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			if (sources[0] instanceof Collection && sources[1] instanceof String) {

				final ConfigurationProvider config = StructrApp.getConfiguration();
				final List extraction = new LinkedList();
				final String keyName = (String)sources[1];

				for (final Object obj : (Collection)sources[0]) {

					if (obj instanceof GraphObject) {

						final PropertyKey key = config.getPropertyKeyForJSONName(obj.getClass(), keyName);
						final Object value = ((GraphObject)obj).getProperty(key);
						if (value != null) {

							extraction.add(value);
						}
					}
				}

				return extraction;
			}
		}

		return null;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_EXTRACT;
	}

	@Override
	public String shortDescription() {
		return "Returns a collection of all the elements with a given name from a collection";
	}

}
