/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;

/**
 * The base class for all structr exceptions. This class contains
 * a buffer that supports context-dependent, nested error
 * messages.
 *
 *
 */
public class FrameworkException extends Exception {

	private final ErrorBuffer errorBuffer = new ErrorBuffer();
	private String message                = null;
	private int status                    = HttpServletResponse.SC_OK;

	public FrameworkException(final int status, final String message) {
		this(status, message, (ErrorToken)null);
	}

	public FrameworkException(final int status, final String message, final ErrorBuffer errorBuffer) {
		this(status, message, (ErrorToken)null);

		// copy error tokens
		this.errorBuffer.getErrorTokens().addAll(errorBuffer.getErrorTokens());
	}

	public FrameworkException(final int status, final String message, final ErrorToken errorToken) {

		this.status  = status;
		this.message = message;

		if (errorToken != null) {
			this.errorBuffer.add(errorToken);
		}
	}

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder();

		if (errorBuffer != null) {

			for (final Iterator<ErrorToken> it = errorBuffer.getErrorTokens().iterator(); it.hasNext();) {

				final ErrorToken token = it.next();

				buf.append(token);

				if (it.hasNext()) {
					buf.append(", ");
				}
			}

		} else {

			buf.append("FrameworkException(").append(status).append("): ").append(message);
		}

		return buf.toString();

	}

	public JsonElement toJSON() {

		JsonObject container = new JsonObject();
		JsonArray errors     = new JsonArray();

		container.add("code", new JsonPrimitive(getStatus()));
		container.add("message", new JsonPrimitive(getMessage()));

		// add errors if there are any
		if (errorBuffer != null) {

			for (final ErrorToken errorToken : errorBuffer.getErrorTokens()) {

				errors.add(errorToken.toJSON());
			}

			container.add("errors", errors);
		}

		return container;
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

	public int getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
