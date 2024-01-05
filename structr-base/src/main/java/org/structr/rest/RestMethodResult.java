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
package org.structr.rest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the result of a REST HTTP method call, i.e. headers, response
 * code etc.
 *
 *
 */
public class RestMethodResult {

	private static final Logger logger = LoggerFactory.getLogger(RestMethodResult.class.getName());

	private boolean serializeSingleObjectAsCollection = Settings.ForceArrays.getValue(false);
	private Map<String, String> headers               = null;
	private List<Object> content                      = null;
	private Object nonGraphObjectResult               = null;
	private int responseCode                          = 0;
	private String message                            = null;
	private Integer overridenResultCount              = null;

	public RestMethodResult(final int responseCode) {
		this.headers      = new LinkedHashMap<>();
		this.responseCode = responseCode;
	}

	public RestMethodResult(final int responseCode, final String message) {
		headers           = new LinkedHashMap<>();
		this.message      = message;
		this.responseCode = responseCode;
	}

	public RestMethodResult(final int responseCode, final boolean serializeSingleObjectAsCollection) {
		this.headers                           = new LinkedHashMap<>();
		this.responseCode                      = responseCode;
		this.serializeSingleObjectAsCollection = serializeSingleObjectAsCollection;
	}

	public void addHeader(final String key, final String value) {
		headers.put(key, value);
	}

	public void addContent(final Object graphObject) {

		if (this.content == null) {
			this.content = new LinkedList<>();
		}

		this.content.add(graphObject);
	}

	public void setNonGraphObjectResult(final Object result) {
		this.nonGraphObjectResult = result;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public List<Object> getContent() {
		return content;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getMessage() {
		return message;
	}

	public Object getNonGraphObjectResult() {
		return nonGraphObjectResult;
	}

	public void setOverridenResultCount(final int resultCount) {
		this.overridenResultCount = resultCount;
	}

	public Integer getOverridenResultCount() {
		return this.overridenResultCount;
	}

	// ----- public static methods -----
	public static String jsonError(final int code, final String message) {
		return jsonMessage(code, message, "error");
	}

	public static String jsonMessage(final int code, final String message) {
		return jsonMessage(code, message, "message");
	}

	public static String jsonMessage(final int code, final String message, final String messageKey) {

		StringBuilder buf = new StringBuilder(100);

		buf.append("{\n");
		buf.append("  \"code\" : ").append(code);

		if (message != null) {

			buf.append(",\n  \"").append(messageKey).append("\" : \"").append(StringUtils.replace(message, "\"", "\\\"")).append("\"\n");

		} else {

			buf.append("\n");

		}

		buf.append("}\n");

		return buf.toString();
	}
}
