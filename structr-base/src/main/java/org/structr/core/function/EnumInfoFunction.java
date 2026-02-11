/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EnumInfoFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "enumInfo";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("type, propertyName [, raw]");
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
						final Set<String> enumConstants = enumProperty.getEnumConstants();

						if (rawList) {

							return enumConstants;

						} else {

							final ArrayList<GraphObjectMap> resultList = new ArrayList();

							for (final String value : enumConstants) {

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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${enumInfo(type, enumProperty[, raw])}. Example ${enumInfo('Document', 'documentType')}"),
			Usage.javaScript("Usage: ${{ $.enumInfo(type, enumProperty[, raw])}}. Example ${{ $.enumInfo('Document', 'documentType')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the possible values of an enum property.";
	}

	@Override
	public String getLongDescription() {
		return """
		The default behaviour of this function is to return a list of objects with a single `value` entry that contains the enum value, so it can be used in a repeater to configure HTML select dropdowns etc:
		
		```
		[ { value: 'ExampleEnum1' }, { value: 'ExampleEnum2' }, { value: 'ExampleEnum3' } ]
		```
		
		If the `raw` parameter is set to `true`, a simple list will be returned:
		```
		[ 'ExampleEnum1', 'ExampleEnum2', 'ExampleEnum3' } ]
		```
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("type", "type on which the property is defined"),
			Parameter.mandatory("propertyName", "name of the property"),
			Parameter.optional("raw", "whether to return a raw list of enum values or a list of objects")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.html("""
			<select>
				<option data-structr-meta-data-key="activityType" data-structr-meta-function-query="enumInfo('Activity', 'activityType')">${activityType.value}</option>
			</select>
			""", "Configure an HTML select element with the enum options of a property")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Schema;
	}
}
