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
package org.structr.console.rest;

import io.restassured.response.Response;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.util.Writable;

import java.io.IOException;

/**
 *
 */
public class PutRestCommand extends GetRestCommand {

	static {

		RestCommand.registerCommand("put", PutRestCommand.class);
	}

	@Override
	public void run(final Console console, final Writable writable) throws FrameworkException, IOException {

		final String requestUrl = getBaseUrl() + getBasePath() + uri;

		if (body != null) {

			writable.println("PUT ", requestUrl);

			final Response response = request(console).body(body).put(requestUrl);

			writable.println(response.getStatusLine());
			writable.print(response.asString());

		} else {

			writable.println("Missing <JSON> parameter - refusing to execute PUT request without payload.");

		}
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Executes a REST PUT request.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		writable.println("put <URI> <JSON> - Executes the given PUT request with the given body (optional).");
	}
}
