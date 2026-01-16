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
package org.structr.console.rest;

import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.util.Writable;

import java.io.IOException;

/**
 *
 */
public class DeleteRestCommand extends RestCommand {

	private String uri = null;

	static {

		RestCommand.registerCommand("delete", DeleteRestCommand.class);
		RestCommand.registerCommand("del",    DeleteRestCommand.class);
	}

	@Override
	public void run(final Console console, final Writable writable) throws FrameworkException, IOException {

		final String requestUrl = getBaseUrl() + getBasePath() + uri;
		final Response response = request(console).delete(requestUrl);

		writable.println("DELETE ", requestUrl);
		writable.println(response.getStatusLine());
		writable.print(response.asString());
	}

	@Override
	public boolean parseNext(final String line, final Writable writable) throws IOException {

		if (StringUtils.isNotBlank(line)) {

			this.uri = line;
			return true;

		} else {

			writable.println("Syntax error, missing URI parameter.");
			return false;
		}
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Executes a REST DELETE request.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		writable.println("delete <URI> - Executes the given DELETE request.");
	}
}
