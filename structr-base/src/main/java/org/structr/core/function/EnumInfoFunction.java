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
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.Traits;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;

public class EnumInfoFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_ENUM_INFO    = "Usage: ${enum_info(type, enumProperty[, raw])}. Example ${enum_info('Document', 'documentType')}";
	public static final String ERROR_MESSAGE_ENUM_INFO_JS = "Usage: ${Structr.enum_info(type, enumProperty[, raw])}. Example ${Structr.enum_info('Document', 'documentType')}";

	@Override
	public String getName() {
		return "enum_info";
	}

	@Override
	public String getSignature() {
		return "type, propertyName [, raw]";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 3);

			final String typeName         = sources[0].toString();
			final String enumPropertyName = sources[1].toString();
			final boolean rawList         = (sources.length == 3) ? Boolean.parseBoolean(sources[2].toString()) : false;
			final Traits type             = Traits.of(typeName);

			if (type != null) {

				final PropertyKey key = type.key(enumPropertyName);
				if (key != null) {

					if (key instanceof EnumProperty) {

						final EnumProperty enumProperty = (EnumProperty)key;
						final Class enumType            = enumProperty.getEnumType();
						final Object[] enumConstants    = enumType.getEnumConstants();
						final List<String> valueList    = new ArrayList<>();

						for (final Object constant : enumConstants) {
							valueList.add(constant.toString());
						}

						if (rawList) {

							return valueList;

						} else {

							final ArrayList<GraphObjectMap> resultList = new ArrayList();

							for (final String value : valueList) {

								final GraphObjectMap valueMap = new GraphObjectMap();
								resultList.add(valueMap);

								valueMap.put(new StringProperty("value"), value);

							}

							return resultList;
						}

					} else {

						logger.warn("Error: Not an Enum property \"{}.{}\"", typeName, enumPropertyName);
						return "Not an Enum property " + typeName + "." + enumPropertyName;
					}

				} else {

					logger.warn("Error: Unknown property \"{}.{}\"", typeName, enumPropertyName);
					return "Unknown property " + typeName + "." + enumPropertyName;
				}

			} else {

				logger.warn("Error: Unknown type \"{}\"", typeName);
				return "Unknown type " + typeName;
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ENUM_INFO_JS : ERROR_MESSAGE_ENUM_INFO);
	}

	@Override
	public String shortDescription() {
		return "Returns the enum values as an array";
	}
}
