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
package org.structr.rest.api;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.docs.*;
import org.structr.rest.api.parameter.RESTParameter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public abstract class RESTEndpoint implements Documentable {

	private final Map<String, RESTParameter> parts = new LinkedHashMap<>();
	private final String pathSeparator            = "/";
	private String uniquePath                     = null;
	private Pattern pattern                       = null;

	public RESTEndpoint(final RESTParameter... parameters) {
		initialize(parameters);
	}

	/**
	 * Returns a handler that is capable of handling an API call accepted
	 * by this endpoint, or null of the call cannot be handled by this
	 * endpoint.
	 *
	 * @param call
	 *
	 * @return an APICallHandler, or null
	 *
	 * @throws FrameworkException
	 */
	public abstract RESTCallHandler accept(final RESTCall call) throws FrameworkException;

	/**
	 * Indicates whether this endpoint only matches URLs that
	 * match the pattern exactly, or if arbitrary strings are
	 * allowed after the prefix.
	 *
	 * @return whether this endpoint matches arbitrary paths after the prefix
	 */
	public abstract boolean isWildcardMatch();

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + uniquePath + ")";
	}

	@Override
	public int hashCode() {
		return uniquePath.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return obj != null && this.hashCode() == obj.hashCode();
	}

	public Matcher matcher(final String path) {
		return pattern.matcher(path);
	}

	public RESTCall initializeRESTCall(final Matcher matcher, final String viewName, final boolean isDefaultView, final String userType) {

		final RESTCall call = new RESTCall(matcher.group(), viewName, isDefaultView, userType);
		int group           = 1;

		for (final RESTParameter part : parts.values()) {

			final String value = matcher.group(group++);

			call.put(part.key(), value);

			if (part.includeInSignature()) {

				// allow REST parameter to override resource access signature
				final String signatureOverride = part.staticResourceSignaturePart();
				if (signatureOverride != null) {

					call.addSignaturePart(signatureOverride);

				} else {

					call.addSignaturePart(value);
				}
			}
		}

		if (isWildcardMatch() && matcher.groupCount() >= group) {

			// optional part is the last group
			final String optionalPart = matcher.group(group);
			if (StringUtils.isNotBlank(optionalPart)) {

				// first slash must be removed because it creates an empty first value
				call.addPathParameters(StringUtils.splitPreserveAllTokens(optionalPart.substring(1), "/"));
			}
		}

		return call;
	}

	// ----- private methods -----
	private void initialize(final RESTParameter... parameters) {

		final StringBuilder pathBuffer = new StringBuilder();

		for (final RESTParameter parameter : parameters) {

			parts.put(parameter.key(), parameter);

			pathBuffer.append(pathSeparator);
			pathBuffer.append("(");
			pathBuffer.append(parameter.urlPattern());
			pathBuffer.append(")");
		}

		if (isWildcardMatch()) {

			pathBuffer.append("(/.*)?");
		}

		this.uniquePath = pathBuffer.toString();
		this.pattern    = Pattern.compile(uniquePath);
	}

	// ----- interface Documentable -----

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.RESTEndpoint;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public String getShortDescription() {
		return "";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {

		final List<Parameter> parameterList = new LinkedList<>();

		for (final RESTParameter part : parts.values()) {

			parameterList.add(Parameter.mandatory(part.key(), part.urlPattern()));
		}

		return parameterList;
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of();
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of();
	}

	@Override
	public List<Language> getLanguages() {
		return List.of();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of();
	}

	@Override
	public List<Property> getProperties() {
		return Documentable.super.getProperties();
	}

	@Override
	public List<Setting> getSettings() {
		return Documentable.super.getSettings();
	}

	@Override
	public List<Concept> getParentConcepts() {
		return List.of(Concept.of("topic", "REST endpoints"));
	}

	@Override
	public List<Link> getLinkedConcepts() {
		return new LinkedList<>();
	}
}
