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

	public static final String NOT_AN_ENUM_PROPERTY_WARNING_MESSAGE = "%s(): Not an Enum property '%s.%s'";

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

			if (type == null) {
				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, TypeInfoFunction.UNKNOWN_TYPE_ERROR_MESSAGE.formatted(getName(), typeName));
			}

			if (!type.hasKey(enumPropertyName)) {
				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, PropertyInfoFunction.UNKNOWN_PROPERTY_ERROR_MESSAGE.formatted(getName(), typeName, enumPropertyName));
			}

			final PropertyKey key = type.key(enumPropertyName);

			if (key instanceof EnumProperty enumProperty) {

				final Set<String> enumConstants = enumProperty.getEnumConstants();

				if (rawList) {

					return enumConstants;

				} else {

					final ArrayList<GraphObjectMap> resultList = new ArrayList<>();

					for (final String value : enumConstants) {

						final GraphObjectMap valueMap = new GraphObjectMap();
						resultList.add(valueMap);

						valueMap.put(new StringProperty("value"), value);
					}

					return resultList;
				}

			} else {

				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, NOT_AN_ENUM_PROPERTY_WARNING_MESSAGE.formatted(getName(), typeName, enumPropertyName));
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${enumInfo(type, enumProperty[, raw = false ])}. Example ${enumInfo('Document', 'documentType')}"),
			Usage.javaScript("Usage: ${{ $.enumInfo(type, enumProperty[, raw = false ])}}. Example ${{ $.enumInfo('Document', 'documentType')}}")
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
			[ { value: 'EnumValue1' }, { value: 'EnumValue2' }, { value: 'EnumValue3' } ]
			```

			If the `raw` parameter is set to `true`, a simple list will be returned:
			```
			[ 'EnumValue1', 'EnumValue2', 'EnumValue3' } ]
			```
			""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("type", "type on which the property is defined"),
			Parameter.mandatory("propertyName", "name of the property"),
			Parameter.optional("raw", "whether to return a raw list (`true`) of enum values or a list of objects (`false`). Default: `false`")
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
	public List<String> getNotes() {
		return List.of(
				"If the requested type does not exist, a catchable error is produced (where applicable) and/or null will be returned.",
				"If the requested property does not exist on the given type, a catchable error is produced (where applicable) and/or null will be returned.",
				"If the requested property on the given type is not an enum property, a catchable error is produced (where applicable) and/or null will be returned."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Schema;
	}
}
