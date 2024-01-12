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
package org.structr.console.rest;

import io.restassured.response.Response;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.util.Writable;

import java.io.IOException;

/**
 *
 */
public class GetRestCommand extends RestCommand {

	protected String uri  = null;
	protected String body = null;

	static {

		RestCommand.registerCommand("get", GetRestCommand.class);
	}

	@Override
	public void run(final Console console, final Writable writable) throws FrameworkException, IOException {

		final String requestUrl = getBaseUrl() + getBasePath() + uri;
		final String trimmed    = body != null ? body.trim() : null;
		final Response response = request(console).get(requestUrl);

		if (trimmed != null && trimmed.startsWith("return ")) {

			final Object value = response.jsonPath().get(trimmed.substring(7));
			writable.println(value);

		} else {

			writable.println("GET ", requestUrl);
			writable.println(response.getStatusLine());
			writable.print(response.asString());
		}
	}

	@Override
	public boolean parseNext(final String line, final Writable writable) throws IOException {

		final StringBuilder buf = new StringBuilder();
		boolean singleQuotes    = false;
		boolean doubleQuotes    = false;
		boolean escaped         = false;

		// split quoted string
		for (final char c : line.toCharArray()) {

			if (uri == null) {

				switch (c) {

					case '\\':
						escaped = !escaped;

					case '"':
						if (escaped) {

							buf.append(c);

						} else {

							doubleQuotes = !doubleQuotes;
						}
						break;

					case '\'':
						if (escaped) {

							buf.append(c);

						} else {

							singleQuotes = !singleQuotes;
						}
						break;

					case ' ':
					case '\t':
					case '\n':

						if (escaped) {

							buf.append(c);

						} else {

							// separator
							if (!singleQuotes && !doubleQuotes && uri == null) {

								uri = buf.toString();
								buf.setLength(0);

							} else {

								buf.append(c);
							}
						}
						break;

					default:
						buf.append(c);

				}

			} else {

				// if uri is set, append the remaining characters to the buffer
				// without any parsing; this will form the request body
				buf.append(c);
			}
		}

		if (singleQuotes || doubleQuotes) {

			writable.println("Mismatched ", (doubleQuotes ? "double" : singleQuotes ? "single" : ""), " quotes.");
			return false;

		} else {

			if (uri == null) {

				// string ended without any spaces
				this.uri = buf.toString();

			} else {

				this.body = buf.toString();
			}
		}

		return true;
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Executes a REST GET request and returns the JSON or parts of it.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		writable.println("get <URI> [return <jsonPath>] - Executes the given GET request.");
	}
}
