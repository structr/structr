/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.common.error;

import com.google.gson.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception to be thrown when an unlicensed scripting function is encountered.
 */
public class AssertException extends RuntimeException implements JsonException {

	private final Map<String, String> headers = new LinkedHashMap<>();
	private int statusCode                    = 422;

	public AssertException(final String message, final int statusCode) {

		super(message);

		this.statusCode = statusCode;
	}

	@Override
	public int getStatus() {
		return statusCode;
	}

	@Override
	public Map<String, String> headers() {
		return headers;
	}

	@Override
	public JsonElement toJSON() {

		JsonObject container = new JsonObject();
		JsonArray errors     = new JsonArray();

		container.add("code", new JsonPrimitive(statusCode));
		container.add("message", (getMessage() != null) ? new JsonPrimitive(getMessage()) : JsonNull.INSTANCE);


		return container;
	}
}
