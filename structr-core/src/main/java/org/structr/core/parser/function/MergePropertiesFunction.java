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

import java.util.LinkedHashSet;
import java.util.Set;
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
public class MergePropertiesFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_MERGE_PROPERTIES = "Usage: ${merge_properties(source, target , mergeKeys...)}. Example: ${merge_properties(this, parent, \"eMail\")}";

	@Override
	public String getName() {
		return "merge_properties()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof GraphObject && sources[1] instanceof GraphObject) {

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Set<PropertyKey> mergeKeys = new LinkedHashSet<>();
			final GraphObject source = (GraphObject)sources[0];
			final GraphObject target = (GraphObject)sources[1];
			final int paramCount = sources.length;

			for (int i = 2; i < paramCount; i++) {

				final String keyName = sources[i].toString();
				final PropertyKey key = config.getPropertyKeyForJSONName(target.getClass(), keyName);

				mergeKeys.add(key);
			}

			for (final PropertyKey key : mergeKeys) {

				final Object sourceValue = source.getProperty(key);
				if (sourceValue != null) {

					target.setProperty(key, sourceValue);
				}

			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MERGE_PROPERTIES;
	}

	@Override
	public String shortDescription() {
		return "";
	}

}
