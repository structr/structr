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

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.schema.action.ActionContext;
import org.structr.util.Writable;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 */
public abstract class RestCommand {

	private static final Map<String, Class<? extends RestCommand>> commands = new TreeMap<>();

	private String username = null;
	private String password = null;

	public abstract void run(final Console console, final Writable writable) throws FrameworkException, IOException;
	public abstract boolean parseNext(final String line, final Writable writable) throws IOException;

	public abstract void commandHelp(final Writable writable) throws IOException;
	public abstract void detailHelp(final Writable writable) throws IOException;

	protected void authenticate(final String username, final String password) {
		this.username = username;
		this.password = password;
	}

	protected RequestSpecification request(final Console console) {

		final RequestSpecification req = RestAssured.given();
		final String usernameFromAuth  = console.getUsername();
		final String passwordFromAuth  = console.getPassword();

		req.accept(ContentType.JSON);

		if (StringUtils.isNoneBlank(username, password)) {

			req.header("X-User",     username);
			req.header("X-Password", password);

		} else if (StringUtils.isNoneBlank(usernameFromAuth, passwordFromAuth)) {

			req.header("X-User",     usernameFromAuth);
			req.header("X-Password", passwordFromAuth);
		}

		return req;
	}

	protected String getBaseUrl() {

		return ActionContext.getBaseUrl();
	}

	protected String getBasePath() {
		return StringUtils.removeEnd(Settings.RestServletPath.getValue(), "/*");
	}

	// ----- public static methods -----
	public static Set<String> commandNames() {
		return commands.keySet();
	}

	public static void registerCommand(final String name, final Class<? extends RestCommand> cmd) {
		commands.put(name, cmd);
	}

	public static void run(final Console console, final String line, final Writable writable) throws FrameworkException, IOException {

		final RestCommand cmd = RestCommand.parse(line, writable);
		if (cmd != null) {

			cmd.run(console, writable);
		}
	}

	public static RestCommand parse(final String line, final Writable writable) throws IOException {

		// first (root) command will always be a single word
		final String trimmed    = line.trim();
		final String firstWord  = StringUtils.substringBefore(trimmed, " ");
		final String remaining  = StringUtils.substringAfter(trimmed, " ");

		if (StringUtils.isNotBlank(firstWord)) {

			final RestCommand cmd = getCommand(firstWord);
			if (cmd != null) {

				if (StringUtils.isBlank(remaining) || cmd.parseNext(remaining, writable)) {

					return cmd;
				}

			} else {

				writable.println("Unknown command '" + firstWord + "'.");
			}
		}

		return null;
	}

	public static RestCommand getCommand(final String name) {

		final Class<? extends RestCommand> cls = commands.get(name);
		if (cls != null) {

			try {
				return cls.newInstance();

			} catch (Throwable ignore) {}
		}

		return null;
	}
}
