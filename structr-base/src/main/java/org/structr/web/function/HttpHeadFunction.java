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
package org.structr.web.function;

import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

/**
 *
 */
public class HttpHeadFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "HEAD";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length >= 1 && sources.length <= 3) {

			try {

				String address = sources[0].toString();
				String username = null;
				String password = null;

				switch (sources.length) {

					case 3: password = sources[2].toString();
					case 2: username = sources[1].toString();
						break;
				}

				return headFromUrl(ctx, address, username, password);

			} catch (Throwable t) {

				logException(caller, t, sources);

			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("url [, username, password]]");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.optional("username", "username for the connection"),
			Parameter.optional("password", "password for the connection")
		);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${HEAD(url[, username, password])}. Example: ${HEAD('http://structr.org', 'foo', 'bar')}"),
			Usage.javaScript("Usage: ${{ $.HEAD(url[, username, password]])}}. Example: ${{ $.HEAD('http://structr.org', 'foo', 'bar')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sends an HTTP HEAD request with optional username and password to the given URL and returns the response headers.";
	}

	@Override
	public String getLongDescription() {
		return """
			This function can be used in a script to make an HTTP HEAD request **from within the Structr Server**, triggered by a frontend control like a button etc. The optional username and password parameters can be used to authenticate the request.

			The `HEAD()` function will return a response object with the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			status | HTTP status of the request | Integer |
			headers | Response headers | Map |
			""";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The `HEAD()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `add_header()` (see the related articles for more information).",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls."
		);
	}
}
