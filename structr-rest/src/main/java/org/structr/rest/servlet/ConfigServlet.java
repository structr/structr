/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.Services;
import org.structr.api.config.Settings;
import org.structr.api.config.SettingsGroup;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Document;
import org.structr.api.util.html.Tag;
import org.structr.api.util.html.attr.Href;
import org.structr.api.util.html.attr.Rel;

/**
 *
 * @author Christian Morgner
 */
public class ConfigServlet extends HttpServlet {

	private static final Logger logger                     = LoggerFactory.getLogger(ConfigServlet.class);
	private static final Set<String> authenticatedSessions = new HashSet<>();

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
		response.setContentType("text/html; charset=utf-8");

		try (final PrintWriter writer = new PrintWriter(response.getWriter())) {

			if (isAuthenticated(request)) {

				final Document doc = createConfigDocument(request, writer);
				doc.render();

			} else {

				final Document doc = createLoginDocument(request, writer);
				doc.render();
			}

			writer.append("\n");    // useful newline
			writer.flush();

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final String action = request.getParameter("action");

		switch (action) {

			case "login":
				final String username = request.getParameter("username");
				final String password = request.getParameter("password");

				if ("superadmin".equals(username) && "sehrgeheim".equals(password)) {

					authenticateSession(request);
				}
				break;

			case "logout":
				invalidateSession(request);
				break;

		}

		response.sendRedirect("/");
	}

	// ----- private methods -----
	private Document createConfigDocument(final HttpServletRequest request, final PrintWriter writer) {

		final Document doc = new Document(writer);
		final Tag body     = setupDocument(request, doc);
		final Tag main     = body.block("div").id("main");
		final Tag tabs     = main.block("div").id("configTabs");
		final Tag menu     = tabs.block("ul").id("configTabsMenu");

		for (final SettingsGroup group : Settings.getGroups()) {

			final String key  = group.getKey();
			final String name = group.getName();

			menu.block("li").block("a").id(key + "Menu").attr(new Attr("href", "#" + key)).block("span").text(name);

			// let settings group render itself
			group.render(tabs.block("div").css("tab-content").id(key));
		}

		body.block("script").text("$(function() { $('#configTabs').tabs({}); });");


		return doc;
	}

	private Document createLoginDocument(final HttpServletRequest request, final PrintWriter writer) {

		final Document doc = new Document(writer);
		final Tag body     = setupDocument(request, doc);

		final Tag loginBox = body.block("div").id("login").css("dialog").attr(new Attr("style", "display: block; margin: auto; margin-top: 200px;"));

		loginBox.block("i").attr(new Attr("title", "Structr Logo")).css("logo-login sprite sprite-structr_gray_100x27");
		loginBox.block("p").text("Welcome to the Structr Configuration Wizard.");

		final Tag form     = loginBox.block("form").attr(new Attr("action", "/"), new Attr("method", "post"));
		final Tag table    = form.block("table");

		final Tag row1     = table.block("tr");
		row1.block("td").block("label").attr(new Attr("for", "usernameField")).text("Username:");
		row1.block("td").empty("input").id("usernameField").attr(new Attr("type", "text"), new Attr("name", "username"));

		final Tag row2     = table.block("tr");
		row2.block("td").block("label").attr(new Attr("for", "passwordField")).text("Password:");
		row2.block("td").empty("input").id("passwordField").attr(new Attr("type", "password"), new Attr("name", "password"));

		final Tag row3     = table.block("tr");
		final Tag cell13   = row3.block("td").attr(new Attr("colspan", "2")).css("btn");
		final Tag button   = cell13.block("button").id("loginButton").attr(new Attr("name", "login"));

		button.block("i").css("sprite sprite-key");
		button.block("span").text(" Login");

		cell13.empty("input").attr(new Attr("type", "hidden"), new Attr("name", "action"), new Attr("value", "login"));

		return doc;
	}

	// ----- private methods -----
	private Tag setupDocument(final HttpServletRequest request, final Document doc) {

		final Tag head = doc.block("head");

		head.block("title").text("Welcome to Structr 2.1");
		head.empty("meta").attr(new Attr("http-equiv", "Content-Type"), new Attr("content", "text/html;charset=utf-8"));
		head.empty("meta").attr(new Attr("name", "viewport"), new Attr("content", "width=1024, user-scalable=yes"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/main.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/lib/jquery-ui-1.10.3.custom.min.css"));
		head.empty("link").attr(new Rel("icon"), new Href("favicon.ico"), new Attr("type", "image/x-icon"));
		head.block("script").attr(new Attr("src", "/structr/js/lib/jquery-1.11.1.min.js"));
		head.block("script").attr(new Attr("src", "/structr/js/lib/jquery-ui-1.11.0.custom.min.js"));

		final Tag body = doc.block("body");
		final Tag header = body.block("div").id("header");

		header.block("i").attr(new Attr("class", "logo sprite sprite-structr-logo"));
		final Tag links = header.block("div").id("menu").css("menu").block("ul");

		if (isAuthenticated(request) && Services.getInstance().isConfigured()) {

			final Tag form = links.block("li").block("form").attr(new Attr("action", "/"), new Attr("method", "post"), new Attr("style", "display: none")).id("logout-form");

			form.block("input").attr(new Attr("type", "hidden"), new Attr("name", "action"), new Attr("value", "logout"));
			links.block("a").text("Logout").attr(new Attr("style", "cursor: pointer"), new Attr("onclick", "$('#logout-form').submit();"));
		}

		return body;
	}

	private boolean isAuthenticated(final HttpServletRequest request) {

		// only display login dialog if a configuration exists (i.e. this is NOT the first run of Structr)
		if (!Services.getInstance().isConfigured()) {
			return true;
		}

		final HttpSession session = request.getSession();
		if (session != null) {

			final String sessionId = session.getId();
			if (sessionId != null) {

				return authenticatedSessions.contains(sessionId);

			} else {

				logger.warn("Cannot check HTTP session without session ID, ignoring.");
			}

		} else {

			logger.warn("Cannot check HTTP request, no session.");
		}

		return false;
	}

	private void authenticateSession(final HttpServletRequest request) {

		final HttpSession session = request.getSession();
		if (session != null) {

			final String sessionId = session.getId();
			if (sessionId != null) {

				authenticatedSessions.add(sessionId);

			} else {

				logger.warn("Cannot authenticate HTTP session without session ID, ignoring.");
			}

		} else {

			logger.warn("Cannot authenticate HTTP request, no session.");
		}
	}

	private void invalidateSession(final HttpServletRequest request) {

		final HttpSession session = request.getSession();
		if (session != null) {

			final String sessionId = session.getId();
			if (sessionId != null) {

				authenticatedSessions.remove(sessionId);

			} else {

				logger.warn("Cannot invalidate HTTP session without session ID, ignoring.");
			}

		} else {

			logger.warn("Cannot invalidate HTTP request, no session.");
		}
	}
}