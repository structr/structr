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
package org.structr.web.traits.wrappers;

import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.polyglot.StructrBinding;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.path.PagePath;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.error.ParseFailureException;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;
import org.structr.web.traits.definitions.PagePathTraitDefinition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PagePathTraitWrapper extends AbstractNodeTraitWrapper implements PagePath {

	public static String CATCH_ALL_ROUTE_NO_REQUIRED_PARAMS_WARNING   = "The absence of static path elements and required parameters makes this route a catch-all.";
	public static String CATCH_ALL_ROUTE_WITH_REQUIRED_PARAMS_WARNING = "The absence of static path elements makes this route a catch-all, matching any URL that provides the required parameters.";
	public static String DUPLICATE_PARAMETER_WARNING                  = "Parameter '%s' occurs multiple times. This will not work.";
	public static String CONFLICTING_PARAMETER_WARNING                = "Parameter '%s' conflicts with builtin functionality. Please choose a different name.";
	public static String PARAMETER_PATTERN_MISMATCH_WARNING           = "'%s' does not match the required path parameter pattern '%s' - it will be treated as a literal.";
	public static String PARAMETER_SHADOWS_ORIGINAL_VALUE_WARNING     = "Parameter '_%s' conflicts with the original value of parameter '%s'. It is recommended to choose a different name.";

	public PagePathTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Page getPage() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(PagePathTraitDefinition.PAGE_PROPERTY));
		if (node != null) {

			return node.as(Page.class);
		}

		return null;
	}

	@Override
	public Integer getPriority() {
		return wrappedObject.getProperty(traits.key(PagePathTraitDefinition.PRIORITY_PROPERTY));
	}

	@Override
	public Iterable<PagePathParameter> getParameters() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(PagePathTraitDefinition.PARAMETERS_PROPERTY);

		return Iterables.map(n -> n.as(PagePathParameter.class), wrappedObject.getProperty(key));
	}


	@Override
	public String[] getWarnings() {

		final String[] warnings = wrappedObject.getProperty(traits.key(PagePathTraitDefinition.WARNINGS_PROPERTY));

		if (warnings == null) {
			return new String[0];
		}

		return warnings;
	}

	@Override
	public Object updatePathAndParameters(final SecurityContext securityContext, final Map<String, Object> arguments) throws FrameworkException {

		final Traits traits         = Traits.of(StructrTraits.PAGE_PATH_PARAMETER);
		final Object rawPath        = arguments.get("path");
		final List<String> warnings = new ArrayList<>();

		if (rawPath instanceof String path) {

			wrappedObject.setProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), path);

			final App app                                   = StructrApp.getInstance(securityContext);
			final StructrBinding structrBinding             = new StructrBinding(new ActionContext(securityContext), null);
			final Map<String, PagePathParameter> parameters = getMappedParameters();
			final List<String> names                        = new ArrayList<>();
			final List<String> toRemove                     = new LinkedList<>(parameters.keySet());

			// extract path parameters and include messages for names that are not allowed
			final Matcher pathParameterBaseMatcher = PATH_PARAMETER_BASE_PATTERN.matcher(path);

			while (pathParameterBaseMatcher.find()) {

				final String baseMatch             = pathParameterBaseMatcher.group(1);
				final Matcher pathParameterMatcher = PATH_PARAMETER_PATTERN.matcher(baseMatch);

				if (pathParameterMatcher.find()) {

					final String paramName = pathParameterMatcher.group(1);

					if (names.contains(paramName)) {

						warnings.add(DUPLICATE_PARAMETER_WARNING.formatted(paramName));
					}

					names.add(paramName);

					if (structrBinding.getMember(paramName) != null) {

						warnings.add(CONFLICTING_PARAMETER_WARNING.formatted(paramName));

					} else if (RenderContext.isRenderContextKeyword(paramName)) {

						warnings.add(CONFLICTING_PARAMETER_WARNING.formatted(paramName));
					}

				} else {

					warnings.add(PARAMETER_PATTERN_MISMATCH_WARNING.formatted(baseMatch, PATH_PARAMETER_PATTERN));
				}
			}

			int count = 0;
			for (final String parameterName : names) {

				toRemove.remove(parameterName);

				// parameter doesn't exist yet, create
				if (!parameters.containsKey(parameterName)) {

					app.create(StructrTraits.PAGE_PATH_PARAMETER,
							new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), parameterName),
							new NodeAttribute<>(traits.key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY), PagePathParameterTraitDefinition.PathParameterValueType.String.name()),
							new NodeAttribute<>(traits.key(PagePathParameterTraitDefinition.POSITION_PROPERTY), count),
							new NodeAttribute<>(traits.key(PagePathParameterTraitDefinition.PATH_PROPERTY), wrappedObject)
					);

				} else {

					final PagePathParameter p = parameters.get(parameterName);
					p.setPosition(count);
				}

				count++;
			}

			// remove parameters that are no longer present in the list
			for (final String parameterName : toRemove) {
				app.delete(parameters.get(parameterName));
			}

			if (!names.isEmpty()) {

				// check for catch-all
				{
					final String staticPathElements = PATH_PARAMETER_PATTERN.matcher(path).replaceAll("").replaceAll("/", "");

					final List<PagePathParameter> pathParameters = Iterables.toList(getParameters());
					final boolean hasAnyMandatoryParameter       = pathParameters.stream().anyMatch(PagePathParameter::getIsRequired);

					if (staticPathElements.isEmpty()) {

						if (hasAnyMandatoryParameter) {

							warnings.addFirst(CATCH_ALL_ROUTE_WITH_REQUIRED_PARAMS_WARNING);

						} else {

							warnings.addFirst(CATCH_ALL_ROUTE_NO_REQUIRED_PARAMS_WARNING);
						}
					}
				}

				// check for situations where a key could override the original value of another key
				for (final String parameterName : names) {

					if (names.contains("_" + parameterName)) {

						warnings.add(PARAMETER_SHADOWS_ORIGINAL_VALUE_WARNING.formatted(parameterName, parameterName));
					}
				}
			}

		} else {

			throw new FrameworkException(422, "Missing or invalid argument: path");
		}

		final List<PagePathParameter> sortedParameters = new LinkedList<>(getMappedParameters().values());

		Collections.sort(sortedParameters, new DefaultSortOrder(traits.key(PagePathParameterTraitDefinition.POSITION_PROPERTY), false));

		this.setProperty(this.getTraits().key(PagePathTraitDefinition.WARNINGS_PROPERTY), warnings.toArray(new String[0]));

		return Map.of(
				"parameters", sortedParameters,
				"warnings", warnings
		);
	}

	@Override
	public Map<String, PagePathParameter> getMappedParameters() {

		final Map<String, PagePathParameter> map = new LinkedHashMap<>();
		final List<PagePathParameter> sorted     = Iterables.toList(getParameters());
		final Traits traits                      = Traits.of(StructrTraits.PAGE_PATH_PARAMETER);

		// sort by position
		Collections.sort(sorted, new DefaultSortOrder(traits.key(PagePathParameterTraitDefinition.POSITION_PROPERTY), false));

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
	@Override
	public Map<String, Object> tryResolvePath(final SecurityContext securityContext, final String[] requestParts) {

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

				final Matcher pathMatcher = PATH_PARAMETER_PATTERN.matcher(pathPart);
				final String requestPart  = getValueOrNull(requestParts, index);

				// does the path part contain a parameter definition?
				if (pathMatcher.find()) {

					final String valueCapturePatternSource = PATH_PARAMETER_PATTERN.matcher(pathPart).replaceAll(result -> {

						final String key                  = result.group(1);
						final PagePathParameter parameter = parameters.get(key);

						if (parameter != null && parameter.getIsRequired()) {
							return "(.+)";
						}

						return "(.*)";
					});
					final Pattern valueCapturePattern      = Pattern.compile("\\A" + valueCapturePatternSource + "\\z");
					final Matcher valueCaptureMatcher      = valueCapturePattern.matcher((requestPart == null ? "" : requestPart));

					if (!valueCaptureMatcher.matches()) {

						return null;

					} else {

						final String[] rawValues = getValues(valueCaptureMatcher);
						int valueIndex           = 0;

						// reset so we find the first key again
						pathMatcher.reset();

						while (pathMatcher.find()) {

							final String rawValue             = getValueOrNull(rawValues, valueIndex++);
							final String key                  = pathMatcher.group(1);
							final PagePathParameter parameter = parameters.get(key);

							// always make the original value available
							arguments.put("_" + key, rawValue);

							if (parameter != null) {

								Object converted     = null;
								boolean inputInvalid = false;

								try {

									converted = parameter.convert(securityContext, rawValue);

								} catch (ParseFailureException e) {

									inputInvalid = true;
								}

								if (converted == null) {

									boolean rawValueMissing = (rawValue == null || rawValue.isEmpty());

									if (rawValueMissing || (inputInvalid && parameter.getUseDefaultIfInvalid())) {

										try {

											final String defaultValue = parameter.getDefaultValue();

											if (defaultValue != null && !defaultValue.isEmpty()) {

												converted = parameter.convert(securityContext, defaultValue);
											}

										} catch (ParseFailureException e) {

											// parsing failure in default value - no more fallbacks
										}
									}
								}

								// put value, even if it is null
								arguments.put(key, converted);

							} else {

								LoggerFactory.getLogger(PagePath.class).warn("No PagePathParameter found for name '{}' in PagePath '{}', treating as string type.", key, getUuid());

								arguments.put(key, rawValue);
							}
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

	@Override
	public String[] getValues(final Matcher matcher) {

		final int groupCount = matcher.groupCount();
		final String[] list  = new String[groupCount];

		for (int i=0; i<groupCount; i++) {

			list[i] = matcher.group(i+1);
		}

		return list;
	}

	@Override
	public String getValueOrNull(final String[] array, final int index) {

		if (array.length > index) {

			final String value = array[index];

			if (!value.isEmpty()) {

				return value;
			}
		}

		return null;
	}
}
