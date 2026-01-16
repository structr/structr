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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a single REST call made against a RESTEndpoint. Contains the raw
 * string-based request values that must be converted to actual typed values.
 */
public class RESTCall extends LinkedHashMap<String, String> {

	private final List<String> pathParameters = new LinkedList<>();
	private final List<String> signatureParts = new LinkedList<>();
	private String userType                   = null;
	private String viewName                   = null;
	private String url                        = null;
	private boolean isDefaultView             = false;

	public RESTCall(final String url, final String viewName, final boolean isDefaultView, final String userType) {

		this.isDefaultView     = isDefaultView;
		this.viewName          = viewName;
		this.userType          = userType;
		this.url               = url;
	}

	public String getResourceSignature() {
		return StringUtils.join(signatureParts, "/");
	}

	public String getURL() {
		return url;
	}

	public String getViewName() {
		return viewName;
	}

	public boolean isDefaultView() {
		return isDefaultView;
	}

	public void addPathParameters(final String[] parts) {
		pathParameters.addAll(Arrays.asList(parts));
	}

	public List<String> getPathParameters() {
		return pathParameters;
	}

	public String getUserType() {
		return userType;
	}

	public void addSignaturePart(final String value) {
		signatureParts.add(value);
	}
}
