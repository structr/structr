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
package org.structr.web.entity.path;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.web.entity.dom.Page;

/**
 *
 */
public interface PagePath extends NodeInterface {

	public static final Pattern PATH_COMPONENT_PATTERN = Pattern.compile("\\{([a-z][a-zA-Z0-9]+)\\}");

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type  = schema.addType("PagePath");
		final JsonObjectType param = schema.addType("PagePathParameter");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/PagePath"));

		// override name property in AbstractNode to make it required
		type.addStringProperty("name", PropertyView.Public, PropertyView.Ui).setRequired(true);

		type.relate(param, "HAS_PARAMETER", Cardinality.OneToMany, "path", "parameters");

		type.addViewProperty(PropertyView.Public, "parameters");
		type.addViewProperty(PropertyView.Ui,     "parameters");

		type.addPropertyGetter("page",       Page.class);
		type.addPropertyGetter("parameters", Iterable.class);

		final JsonObjectType page = schema.addType("Page");
		page.relate(type, "HAS_PATH", Cardinality.OneToMany, "page", "paths");

		//type.overrideMethod("onCreation",     true,  PagePath.class.getName() + ".onCreation(this, arg0, arg1);");
		//type.overrideMethod("onModification", true,  PagePath.class.getName() + ".onModification(this, arg0, arg1, arg2);");
	}}

	// implemented by methods created with addPropertyGetter above
	Page getPage();
	Iterable<PagePathParameter> getParameters();

	// ----- default methods -----
	default Map<String, PagePathParameter> getMappedParameters() {

		final Map<String, PagePathParameter> map = new LinkedHashMap<>();

		for (final PagePathParameter param : getParameters()) {

			map.put(param.getName(), param);
		}

		return map;
	}

	default Map<String, Object> tryResolvePath(final String[] requestParts) {

		/**
		 * TODO:
		 *  - split name attribute on /
		 *  - walk over components of name attribute
		 *  - match request parts (or use default value)
		 *  - first part must be static
		 *  - insert default values for missing parameters
		 *  -
		 *
		 * Disadvantage of parameter-only: no prefixes possible, or things like /prefix_{key1}_{key2}_{key3}, so path might be better
		 *
		 */

		final Map<String, Object> arguments = new LinkedHashMap<>();


		String path = getName();
		if (path != null) {

			if (path.startsWith("/")) {
				path = path.substring(1);
			}

			final Map<String, PagePathParameter> parameters = getMappedParameters();
			final String[] parts                            = path.split("/");
			int index                                       = 0;

			for (final String pathPart : parts) {

				final Matcher pathMatcher = PATH_COMPONENT_PATTERN.matcher(pathPart);
				final String requestPart  = getValueOrNull(requestParts, index);

				// does the path part contain a parameter definition?
				if (pathMatcher.find()) {

					final String valueCapturePatternSource = PATH_COMPONENT_PATTERN.matcher(pathPart).replaceAll("(.*)");
					final Pattern valueCapturePattern      = Pattern.compile(valueCapturePatternSource);
					final Matcher valueCaptureMatcher      = valueCapturePattern.matcher(StringUtils.defaultIfBlank(requestPart, ""));
					final String[] rawValues               = getValues(valueCaptureMatcher);
					int valueIndex                         = 0;

					// reset so we find the first key again
					pathMatcher.reset();

					while (pathMatcher.find()) {

						final String rawValue             = getValueOrNull(rawValues, valueIndex++);
						final String key                  = pathMatcher.group(1);
						final PagePathParameter parameter = parameters.get(key);

						if (parameter != null) {

							final String defaultValue = parameter.getDefaultValue();
							final Object converted = parameter.convert(rawValue);

							if (converted != null) {

								// only put value if conversion is successful
								arguments.put(key, converted);

							} else {

								// put default value otherwise
								arguments.put(key, defaultValue);
							}

						} else {

							// no matching parameter for key ""...
							arguments.put(key, rawValue);
						}
					}

				} else {

					// no parameter definition => path parts must be identical
					if (!pathPart.equals(requestPart)) {

						// no match, return early
						return null;
					}
				}

				index++;
			}
		}

		return arguments;
	}

	default String[] getValues(final Matcher matcher) {

		if (matcher.find()) {

			final int groupCount = matcher.groupCount();
			final String[] list  = new String[groupCount];

			for (int i=0; i<groupCount; i++) {

				list[i] = matcher.group(i+1);
			}

			return list;
		}

		return new String[0];
	}

	default String getValueOrNull(final String[] array, final int index) {

		if (array.length > index) {
			return array[index];
		}

		return null;
	}
}
