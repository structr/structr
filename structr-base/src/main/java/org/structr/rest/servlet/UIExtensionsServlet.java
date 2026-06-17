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
package org.structr.rest.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.module.StructrModule;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Serves /structr/js/module-extensions.js — a dynamically generated script
 * that uses document.write() to synchronously inject UI extension scripts
 * registered by loaded StructrModules via {@link StructrModule#getUIExtensionScripts()}.
 */
public class UIExtensionsServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(UIExtensionsServlet.class);

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		response.setContentType("application/javascript; charset=UTF-8");
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

		try (final PrintWriter out = response.getWriter()) {

			for (final StructrModule module : StructrApp.getConfiguration().getModules().values()) {

				final List<String> scripts = module.getUIExtensionScripts();
				logger.debug("UIExtensions: module '{}' ({}) -> scripts: {}", module.getName(), module.getClass().getName(), scripts);

				if (scripts != null && !scripts.isEmpty()) {

					out.println("/* " + module.getName() + " */");

					for (final String script : scripts) {

						out.println("document.write('<script src=\"/structr/" + script + "\"><\\/script>');");
					}
				}
			}
		}
	}
}
