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

import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.internal.util.Iterables;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.*;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.PageHAS_PATHPagePath;
import org.structr.web.entity.dom.relationship.PagePathHAS_PARAMETERPagePathParameter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class PagePath extends AbstractNode {

	public static final Pattern PATH_COMPONENT_PATTERN = Pattern.compile("\\{([a-z][a-zA-Z0-9]+)\\}");

	public static Property<Page> pageProperty                              = new StartNode<>("page", PageHAS_PATHPagePath.class).partOfBuiltInSchema();
	public static Property<Iterable<PagePathParameter>> parametersProperty = new EndNodes<>("parameters", PagePathHAS_PARAMETERPagePathParameter.class).partOfBuiltInSchema();
	public static Property<String> nameProperty                            = new StringProperty("name").notNull().partOfBuiltInSchema();
	public static Property<Integer> priorityProperty                       = new IntProperty("priority").partOfBuiltInSchema();

	public static final View defaultView = new View(PagePath.class, PropertyView.Public,
		nameProperty, priorityProperty, parametersProperty
	);

	public static final View uiView = new View(PagePath.class, PropertyView.Ui,
		nameProperty, priorityProperty, parametersProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, PagePath.nameProperty, errorBuffer);

		return valid;
	}

	public Page getPage() {
		return getProperty(pageProperty);
	}

	public Iterable<PagePathParameter> getParameters() {
		return getProperty(parametersProperty);
	}

	// ----- default methods -----
	@Export
	public Object updatePathAndParameters(final SecurityContext securityContext, final Map<String, Object> arguments) throws FrameworkException {

		final Object rawList = arguments.get("names");
		final Object rawPath = arguments.get("path");

		if (rawPath instanceof String path) {

			if (rawList instanceof List rawNames) {

				// update path
				this.setProperty(AbstractNode.name, path);

				// update parameters
				final App app                                   = StructrApp.getInstance(securityContext);
				final Map<String, PagePathParameter> parameters = getMappedParameters();
				final List<String> names                        = (List<String>)rawNames;
				final List<String> toRemove                     = new LinkedList<>(parameters.keySet());
				int count                                       = 0;

				for (final String parameterName : names) {

					// parameter doesn't exist yet, create
					if (!parameters.containsKey(parameterName)) {

						app.create(PagePathParameter.class,
							new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "name"), parameterName),
							new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "valueType"), "String"),
							new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "position"), count),
							new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "path"), this)
						);

					} else {

						final PagePathParameter p = parameters.get(parameterName);
						p.setPosition(count);
					}

					toRemove.remove(parameterName);

					count++;
				}

				// remove parameters that are no longer present in the list
				for (final String parameterName : toRemove) {
					app.delete(parameters.get(parameterName));
				}

				// update positions
				final Map<String, PagePathParameter> updatedParameters = getMappedParameters();
				int index                                              = 0;

				for (final String parameterName : names) {

					final PagePathParameter param = updatedParameters.get(parameterName);
					param.setPosition(index++);
				}

			} else {

				throw new FrameworkException(422, "Missing or invalid argument: names");
			}

		} else {

			throw new FrameworkException(422, "Missing or invalid argument: path");
		}

		final List<PagePathParameter> sortedParameters = new LinkedList<>(getMappedParameters().values());

		Collections.sort(sortedParameters, new DefaultSortOrder(StructrApp.key(PagePathParameter.class, "position"), false));

		return sortedParameters;
	}


	public Map<String, PagePathParameter> getMappedParameters() {

		final Map<String, PagePathParameter> map = new LinkedHashMap<>();
		final List<PagePathParameter> sorted     = Iterables.asList(getParameters());

		// sort by position
		Collections.sort(sorted, new DefaultSortOrder(StructrApp.key(PagePathParameter.class, "position"), false));

		for (final PagePathParameter param : getParameters()) {

			map.put(param.getName(), param);
		}

		return map;
	}

	/**
	 * Tries to match the given request parts (URL path components that
	 * are already split) and returns the resolved arguments. Returns
	 * null if the path doesn't match.
	 *
	 * @param requestParts the URL path components
	 * @return the resolved arguments, or null if the path doesn't match
	 */
	public Map<String, Object> tryResolvePath(final String[] requestParts) {

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

		} else {

			LoggerFactory.getLogger(PagePath.class).warn("PagePath with ID {} has no name attribute, ignoring.", getUuid());
		}

		return arguments;
	}

	public String[] getValues(final Matcher matcher) {

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

	public String getValueOrNull(final String[] array, final int index) {

		if (array.length > index) {

			final String value = array[index];

			// return empty strings as null
			if (StringUtils.isNotBlank(value)) {

				return value;
			}
		}

		return null;
	}
}
