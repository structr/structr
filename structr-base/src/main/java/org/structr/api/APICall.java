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
package org.structr.api;

import java.util.LinkedHashMap;
import org.structr.api.parameter.APIParameter;

/**
 * Represents a single call made against an APIEndpoint. Contains the raw
 * string values from the URL that must be converted to actual typed values.
 */
public class APICall extends LinkedHashMap<APIParameter, String> {

	private APIEndpoint endpoint = null;
	private String viewName      = null;
	private String url           = null;

	public APICall(final APIEndpoint endpoint, final String url, final String viewName) {

		this.endpoint = endpoint;
		this.viewName = viewName;
		this.url      = url;
	}

	public APIEndpoint getEndpoint() {
		return endpoint;
	}

	public String getURL() {
		return url;
	}

	public String getViewName() {
		return viewName;
	}
}
