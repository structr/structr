/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import java.util.LinkedHashMap;
import org.structr.rest.api.parameter.RESTParameter;

/**
 * Represents a single REST call made against a RESTEndpoint. Contains the raw
 * string-based request values that must be converted to actual typed values.
 */
public class RESTCall extends LinkedHashMap<RESTParameter, String> {

	private String viewName      = null;
	private String url           = null;

	public RESTCall(final String url, final String viewName) {

		this.viewName = viewName;
		this.url      = url;
	}

	public String getURL() {
		return url;
	}

	public String getViewName() {
		return viewName;
	}
}
