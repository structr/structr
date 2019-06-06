/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.config.SettingsGroup;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Document;
import org.structr.api.util.html.Tag;
import org.structr.api.util.html.attr.Href;
import org.structr.api.util.html.attr.Rel;
import org.structr.core.Services;

/**
 *
 */
public class ConfigServlet extends AbstractServletBase {

	private static final Logger logger                     = LoggerFactory.getLogger(ConfigServlet.class);
	private static final Set<String> authenticatedSessions = new HashSet<>();
	private static final String ConfigUrl                  = "/structr/config";
	private static final String ConfigName                 = "structr.conf";
	private static final String TITLE                      =  "Structr Configuration Editor";

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		setCustomResponseHeaders(response);

		if (!isAuthenticated(request)) {

			// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
			response.setContentType("text/html; charset=utf-8");

			try (final PrintWriter writer = new PrintWriter(response.getWriter())) {

				final Document doc = createLoginDocument(request, writer);
				doc.render();

				writer.append("\n");
				writer.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

		} else {

			if (request.getParameter("reload") != null) {

				// reload data
				Settings.loadConfiguration(ConfigName);

				// redirect
				response.sendRedirect(ConfigUrl);

			} else if (request.getParameter("reset") != null) {

				final String key      = request.getParameter("reset");
				Setting setting = Settings.getSetting(key);

				if (setting == null) {
					setting = Settings.getCaseSensitiveSetting(key);
				}

				if (setting != null) {

					if (setting.isDynamic()) {

						// remove
						setting.unregister();

					} else {

						// reset to default
						setting.setValue(setting.getDefaultValue());
					}
				}

				// serialize settings
				Settings.storeConfiguration(ConfigName);

				// redirect
				response.sendRedirect(ConfigUrl);

			} else if (request.getParameter("start") != null) {

				final String serviceName = request.getParameter("start");
				if (serviceName != null && isAuthenticated(request)) {

					Services.getInstance().startService(serviceName);
				}

				// redirect
				response.sendRedirect(ConfigUrl + "#services");

			} else if (request.getParameter("stop") != null) {

				final String serviceName = request.getParameter("stop");
				if (serviceName != null && isAuthenticated(request)) {

					Services.getInstance().shutdownService(serviceName);
				}

				// redirect
				response.sendRedirect(ConfigUrl + "#services");

			} else if (request.getParameter("restart") != null) {

				final String serviceName = request.getParameter("restart");
				if (serviceName != null && isAuthenticated(request)) {

					new Thread(new Runnable() {

						@Override
						public void run() {

							try { Thread.sleep(1000); } catch (Throwable t) {}

							Services.getInstance().shutdownService(serviceName);
							Services.getInstance().startService(serviceName);
						}

					}).start();
				}

				// redirect
				response.sendRedirect(ConfigUrl + "#services");

			} else {

				// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
				response.setContentType("text/html; charset=utf-8");

				try (final PrintWriter writer = new PrintWriter(response.getWriter())) {

					final Document doc = createConfigDocument(request, writer);
					doc.render();

					writer.append("\n");
					writer.flush();

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}

			}
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		setCustomResponseHeaders(response);

		final String action   = request.getParameter("action");
		String redirectTarget = "";

		if (action != null) {

			switch (action) {

				case "login":
					
					if (StringUtils.isNoneBlank(Settings.SuperUserPassword.getValue(), request.getParameter("password")) && Settings.SuperUserPassword.getValue().equals(request.getParameter("password"))) {
						authenticateSession(request);
					}
					break;

				case "logout":
					invalidateSession(request);
					break;

			}

		} else if (isAuthenticated(request)) {

			// a configuration form was submitted
			for (final Entry<String, String[]> entry : request.getParameterMap().entrySet()) {

				final String value   = getFirstElement(entry.getValue());
				final String key     = entry.getKey();
				SettingsGroup parent = null;

				// skip internal group configuration parameter
				if (key.endsWith("._settings_group")) {
					continue;
				}

				if ("active_section".equals(key)) {

					redirectTarget = value;
					continue;
				}

				Setting<?> setting = Settings.getSetting(key);

				if (setting != null && setting.isDynamic()) {

					// unregister dynamic settings so the type can change
					setting.unregister();
					setting = null;
				}

				if (setting == null) {

					if (key.contains(".cronExpression")) {

						parent = Settings.cronGroup;

					} else {

						// group specified?
						final String group = request.getParameter(key + "._settings_group");
						if (group != null) {

							parent = Settings.getGroup(group);
							if (parent == null) {

								// default to misc group
								parent = Settings.miscGroup;
							}

						} else {

							// fallback to misc group
							parent = Settings.miscGroup;
						}
					}

					setting = Settings.createSettingForValue(parent, key, value);
				}

				// store new value
				setting.fromString(value);
			}

			// serialize settings
			Settings.storeConfiguration(ConfigName);
		}

		response.sendRedirect(ConfigUrl + redirectTarget);
	}

	// ----- private methods -----
	private Document createConfigDocument(final HttpServletRequest request, final PrintWriter writer) {

		final Document doc = new Document(writer);
		final Tag body     = setupDocument(request, doc);
		final Tag form     = body.block("form").css("config-form");
		final Tag main     = form.block("div").id("main");
		final Tag tabs     = main.block("div").id("configTabs");
		final Tag menu     = tabs.block("ul").css("tabs-menu");

		// configure form
		form.attr(new Attr("action", ConfigUrl), new Attr("method", "post"));

		for (final SettingsGroup group : Settings.getGroups()) {

			final String key  = group.getKey();
			final String name = group.getName();

			menu.block("li").block("a").id(key + "Menu").attr(new Attr("href", "#" + key)).block("span").text(name);

			final Tag container = tabs.block("div").css("tab-content").id(key);

			// let settings group render itself
			group.render(container);

			// stop floating
			container.block("div").attr(new Style("clear: both;"));
		}

		// add services tab
		menu.block("li").block("a").id("servicesMenu").attr(new Attr("href", "#services")).block("span").text("Services");

		final Services services = Services.getInstance();
		final Tag container     = tabs.block("div").css("tab-content").id("services");
		final Tag table         = container.block("table").id("services-table");
		final Tag header        = table.block("tr");

		header.block("th").text("Service Name");
		header.block("th").attr(new Attr("colspan", "2"));


		for (final Class serviceClass : services.getServices()) {

			final boolean running         = serviceClass != null ? services.isReady(serviceClass) : false;
			final String serviceClassName = serviceClass.getSimpleName();

			final Tag row  = table.block("tr");

			row.block("td").text(serviceClassName);

			if (running) {

				row.block("td").block("button").attr(new Type("button"), new OnClick("window.location.href='" + ConfigUrl + "?restart=" + serviceClassName + "';")).text("Restart");

				if ("HttpService".equals(serviceClassName)) {

					row.block("td");

				} else {

					row.block("td").block("button").attr(new Type("button"), new OnClick("window.location.href='" + ConfigUrl + "?stop=" + serviceClassName + "';")).text("Stop");
				}

				row.block("td");

			} else {

				row.block("td");
				row.block("td");
				row.block("td").block("button").attr(new Type("button"), new OnClick("window.location.href='" + ConfigUrl + "?start=" + serviceClassName + "';")).text("Start");
			}
		}

		// update active section so we can restore it when redirecting
		container.empty("input").attr(new Type("hidden"), new Name("active_section")).id("active_section");

		// stop floating
		container.block("div").attr(new Style("clear: both;"));

		// buttons
		final Tag buttons = form.block("div").css("buttons");

		buttons.block("button").attr(new Type("button")).id("new-entry-button").text("Add entry");
		buttons.block("button").attr(new Type("button")).id("reload-config-button").text("Reload configuration file");
		buttons.empty("input").attr(new Type("submit"), new Value("Save to structr.conf"));

		return doc;
	}

	private Document createLoginDocument(final HttpServletRequest request, final PrintWriter writer) {

		final Document doc = new Document(writer);
		final Tag body     = setupDocument(request, doc).css("login");

		final Tag loginBox = body.block("div").id("login").css("dialog").attr(new Style("display: block; margin: auto; margin-top: 200px;"));

		loginBox.block("i").attr(new Attr("title", "Structr Logo")).css("logo-login sprite sprite-structr_gray_100x27");
		loginBox.block("p").text("Welcome to the " + TITLE + ". Please log in with the <b>super- user</b> password which can be found in your structr.conf.");

		final Tag form     = loginBox.block("form").attr(new Attr("action", ConfigUrl), new Attr("method", "post"));
		final Tag table    = form.block("table");
		final Tag row1     = table.block("tr");

		row1.block("td").block("label").attr(new Attr("for", "passwordField")).text("Password:");
		row1.block("td").empty("input").attr(new Attr("autofocus", "tur")).id("passwordField").attr(new Type("password"), new Name("password"));

		final Tag row2     = table.block("tr");
		final Tag cell13   = row2.block("td").attr(new Attr("colspan", "2")).css("btn");
		final Tag button   = cell13.block("button").id("loginButton").attr(new Name("login"));

		button.block("i").css("sprite sprite-key");
		button.block("span").text(" Login");

		cell13.empty("input").attr(new Type("hidden"), new Name("action"), new Value("login"));

		return doc;
	}

	// ----- private methods -----
	private Tag setupDocument(final HttpServletRequest request, final Document doc) {

		final Tag head = doc.block("head");

		head.block("title").text(TITLE);
		head.empty("meta").attr(new Attr("http-equiv", "Content-Type"), new Attr("content", "text/html;charset=utf-8"));
		head.empty("meta").attr(new Name("viewport"), new Attr("content", "width=1024, user-scalable=yes"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/lib/jquery-ui-1.10.3.custom.min.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/main.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/sprites.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/config.css"));
		head.empty("link").attr(new Rel("icon"), new Href("favicon.ico"), new Type("image/x-icon"));
		head.block("script").attr(new Src("/structr/js/lib/jquery-3.3.1.min.js"));
		head.block("script").attr(new Src("/structr/js/icons.js"));
		head.block("script").attr(new Src("/structr/js/config.js"));

		final Tag body = doc.block("body");
		final Tag header = body.block("div").id("header");

		header.block("i").css("logo sprite sprite-structr-logo");
		final Tag links = header.block("div").id("menu").css("menu").block("ul");

		if (isAuthenticated(request)) {

			final Tag form = links.block("li").block("form").attr(new Attr("action", ConfigUrl), new Attr("method", "post"), new Style("display: none")).id("logout-form");

			form.empty("input").attr(new Type("hidden"), new Name("action"), new Value("logout"));
			links.block("a").text("Logout").attr(new Style("cursor: pointer"), new OnClick("$('#logout-form').submit();"));
		}

		return body;
	}

	private boolean isAuthenticated(final HttpServletRequest request) {

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

	private String getFirstElement(final String[] values) {

		if (values != null && values.length == 1) {

			return values[0];
		}

		return null;
	}

	// ----- nested classes -----
	private static class Style extends Attr {

		public Style(final Object value) {
			super("style", value);
		}
	}

	private static class Src extends Attr {

		public Src(final Object value) {
			super("src", value);
		}
	}

	private static class Type extends Attr {

		public Type(final Object value) {
			super("type", value);
		}
	}

	private static class Name extends Attr {

		public Name(final Object value) {
			super("name", value);
		}
	}

	private static class Value extends Attr {

		public Value(final Object value) {
			super("value", value);
		}
	}

	private static class OnClick extends Attr {

		public OnClick(final Object value) {
			super("onclick", value);
		}
	}
}