/*
 * Copyright (C) 2010-2021 Structr GmbH
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.config.SettingsGroup;
import org.structr.api.service.DatabaseConnection;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Document;
import org.structr.api.util.html.InputField;
import org.structr.api.util.html.SelectField;
import org.structr.api.util.html.Tag;
import org.structr.api.util.html.attr.Href;
import org.structr.api.util.html.attr.Rel;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.ManageDatabasesCommand;
import org.structr.core.graph.TransactionCommand;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class ConfigServlet extends AbstractServletBase {

	private static final Logger logger                = LoggerFactory.getLogger(ConfigServlet.class);
	private static final Set<String> sessions         = new HashSet<>();
	private static final String TITLE                 = "Structr Configuration Editor";

	private static final String ConfigServletLocation = "/structr/config";
	private static final String AdminBackendLocation  = "/structr/";

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
				logger.error(ExceptionUtils.getStackTrace(ioex));
			}

		} else {

			if (request.getParameter("reload") != null) {

				// reload data
				Settings.loadConfiguration(Settings.ConfigFileName);

				// redirect
				sendRedirectHeader(response, ConfigServletLocation);

			} else if (request.getParameter("reset") != null) {

				final String key = request.getParameter("reset");
				Setting setting  = Settings.getSetting(key);

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
				Settings.storeConfiguration(Settings.ConfigFileName);

				// redirect
				sendRedirectHeader(response, ConfigServletLocation);

			} else if (request.getParameter("start") != null) {

				final String serviceName = request.getParameter("start");
				if (serviceName != null) {

					try {
						Services.getInstance().startService(serviceName);

					} catch (FrameworkException fex) {

						response.setContentType("application/json");
						response.setStatus(fex.getStatus());
						response.getWriter().print(fex.toJSON());
						response.getWriter().flush();
						response.getWriter().close();

						return;
					}
				}

				sendRedirectHeader(response, ConfigServletLocation + "#services");

			} else if (request.getParameter("stop") != null) {

				final String serviceName = request.getParameter("stop");
				if (serviceName != null) {

					Services.getInstance().shutdownService(serviceName);
				}

				sendRedirectHeader(response, ConfigServletLocation + "#services");

			} else if (request.getParameter("restart") != null) {

				final String serviceName = request.getParameter("restart");
				if (serviceName != null) {

					new Thread(new Runnable() {

						@Override
						public void run() {

							try { Thread.sleep(1000); } catch (Throwable t) {}

							Services.getInstance().shutdownService(serviceName);

							try {
								Services.getInstance().startService(serviceName);

							} catch (FrameworkException fex) {

								logger.warn("Unable to start service '{}'", serviceName);
								logger.warn("", fex);
							}
						}
					}).start();
				}

				sendRedirectHeader(response, ConfigServletLocation + "#services");

			} else if (request.getParameter("finish") != null) {

				// finish wizard
				Settings.SetupWizardCompleted.setValue(true);
				Settings.storeConfiguration(Settings.ConfigFileName);

				sendRedirectHeader(response, AdminBackendLocation);

			} else if (request.getParameter("useDefault") != null) {

				// create default configuration
				final ManageDatabasesCommand cmd    = Services.getInstance().command(null, ManageDatabasesCommand.class);
				final String name                   = "neo-1";
				final String url                    = Settings.SampleConnectionUrl.getDefaultValue();
				final String databaseName           = Settings.ConnectionDatabaseName.getDefaultValue();
				final String username               = Settings.ConnectionUser.getDefaultValue();
				final String password               = Settings.ConnectionPassword.getDefaultValue();

				final DatabaseConnection connection = new DatabaseConnection();
				connection.setName(name);
				connection.setUrl(url);
				connection.setUsername(username);
				connection.setPassword(password);
				connection.setDatabaseName(databaseName);

				try {
					cmd.addConnection(connection, false);

				} catch (FrameworkException fex) {
					logger.error(ExceptionUtils.getStackTrace(fex));
				}

				// finish wizard
				Settings.SetupWizardCompleted.setValue(true);
				Settings.storeConfiguration(Settings.ConfigFileName);

				// make session valid
				authenticateSession(request);

				sendRedirectHeader(response, ConfigServletLocation + "#databases");

			} else if (request.getParameter("setMaintenance") != null) {

				final boolean maintenanceEnabled = Boolean.parseBoolean(request.getParameter("setMaintenance"));
				final boolean success            = Services.getInstance().setMaintenanceMode(maintenanceEnabled);

				if (success) {

					final String baseUrl = ActionContext.getBaseUrl(request, true);

					final Map<String, Object> msgData = new HashMap();
					msgData.put(MaintenanceCommand.COMMAND_TYPE_KEY, "MAINTENANCE");
					msgData.put("enabled",                           maintenanceEnabled);
					msgData.put("baseUrl",                           baseUrl);
					TransactionCommand.simpleBroadcastGenericMessage(msgData, Predicate.all());

					sendRedirectHeader(response, ConfigServletLocation + "#maintenance");
				}

			} else {

				// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
				response.setContentType("text/html; charset=utf-8");

				try (final PrintWriter writer = new PrintWriter(response.getWriter())) {

					final Document doc = createConfigDocument(request, writer);
					doc.render();

					writer.append("\n");
					writer.flush();

				} catch (IOException ioex) {
					logger.error(ExceptionUtils.getStackTrace(ioex));
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

			// set redirect target
			redirectTarget = request.getParameter("active_section");

			// database connections form
			if ("/add".equals(request.getPathInfo())) {

				final ManageDatabasesCommand cmd    = Services.getInstance().command(null, ManageDatabasesCommand.class);
				final String name                   = request.getParameter("name");
				final String driver                 = request.getParameter("driver");
				final String url                    = request.getParameter("url");
				final String databaseName           = request.getParameter("database");
				final String username               = request.getParameter("username");
				final String password               = request.getParameter("password");
				final String connectNow             = request.getParameter("now");
				final DatabaseConnection connection = new DatabaseConnection();

				connection.setName(name);
				connection.setDriver(driver);
				connection.setUrl(url);
				connection.setDatabaseName(databaseName);
				connection.setUsername(username);
				connection.setPassword(password);

				try {
					cmd.addConnection(connection, cmd.getConnections().isEmpty() && "true".equals(connectNow));

					// wizard finished
					Settings.SetupWizardCompleted.setValue(true);

					// make session valid
					authenticateSession(request);

				} catch (FrameworkException fex) {

					response.setContentType("application/json");
					response.setStatus(fex.getStatus());
					response.getWriter().print(fex.toJSON());
					response.getWriter().flush();
					response.getWriter().close();

					return;
				}

			} else {

				// check for REST action
				final String path = request.getPathInfo();
				if (StringUtils.isNotBlank(path)) {

					final String[] parts = StringUtils.split(path, "/");
					if (parts.length == 2) {

						final ManageDatabasesCommand cmd = Services.getInstance().command(null, ManageDatabasesCommand.class);
						final Map<String, Object> data   = new LinkedHashMap<>();
						final String name                = parts[0];
						final String restAction          = parts[1];

						// values for save action
						final String driver              = request.getParameter("driver");
						final String connectionUrl       = request.getParameter("url");
						final String databaseName        = request.getParameter("database");
						final String connectionUsername  = request.getParameter("username");
						final String connectionPassword  = request.getParameter("password");

						data.put(DatabaseConnection.KEY_NAME,         name);
						data.put(DatabaseConnection.KEY_DRIVER,       driver);
						data.put(DatabaseConnection.KEY_URL,          connectionUrl);
						data.put(DatabaseConnection.KEY_DATABASENAME, databaseName);
						data.put(DatabaseConnection.KEY_USERNAME,     connectionUsername);
						data.put(DatabaseConnection.KEY_PASSWORD,     connectionPassword);

						try {
							switch (restAction) {

								case "save":
									cmd.saveConnection(data);
									break;

								case "delete":
									cmd.removeConnection(data);
									break;

								case "connect":
									cmd.saveConnection(data);
									cmd.activateConnection(data);
									break;

								case "disconnect":
									cmd.deactivateConnections();
									break;
							}

						} catch (FrameworkException fex) {

							response.setContentType("application/json");
							response.setStatus(fex.getStatus());
							response.getWriter().print(fex.toJSON());
							response.getWriter().flush();
							response.getWriter().close();

							return;
						}
					}

				} else {

					// a configuration form was submitted
					for (final Entry<String, String[]> entry : request.getParameterMap().entrySet()) {

						final String value   = getFirstElement(entry.getValue());
						final String key     = entry.getKey();
						SettingsGroup parent = null;

						// skip internal group configuration parameter
						if (key.endsWith("._settings_group")) {
							continue;
						}

						// skip

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
				}
			}

			// serialize settings
			Settings.storeConfiguration(Settings.ConfigFileName);
		}

		sendRedirectHeader(response, ConfigServletLocation + redirectTarget);
	}

	// ----- private methods -----
	private Document createConfigDocument(final HttpServletRequest request, final PrintWriter writer) {

		final boolean firstStart = !Settings.SetupWizardCompleted.getValue();
		final Document doc       = new Document(writer);
		final Tag body           = setupDocument(request, doc);
		final Tag form           = body.block("form").css("config-form").empty("input").attr(new Attr("type", "submit"), new Attr("disabled", "disabled")).css("hidden").parent();
		final Tag main           = form.block("div").id("main");
		final Tag tabs           = main.block("div").id("configTabs");
		final Tag menu           = tabs.block("ul").css("tabs-menu");

		// configure form
		form.attr(new Attr("action", prefixLocation(ConfigServletLocation)), new Attr("method", "post"));

		if (firstStart) {
			welcomeTab(menu, tabs);
		}

		databasesTab(menu, tabs);

		if (!firstStart) {

			// settings tabs
			for (final SettingsGroup group : Settings.getGroups()) {

				group.render(menu, tabs);
			}

			// services tab
			menu.block("li").block("a").id("servicesMenu").attr(new Attr("href", "#services")).block("span").text("Services");

			final Services services = Services.getInstance();
			final Tag container     = tabs.block("div").css("tab-content").id("services");

			container.block("h1").text("Services");

			final Tag table         = container.block("table").id("services-table");
			final Tag header        = table.block("tr");

			header.block("th").text("Service Name");
			header.block("th").attr(new Attr("colspan", "2"));

			for (final Class serviceClass : services.getRegisteredServiceClasses()) {

				final Set<String> serviceNames = new TreeSet<>();

				serviceNames.addAll(services.getServices(serviceClass).keySet());
				serviceNames.add("default");

				for (final String name : serviceNames) {

					final boolean running         = serviceClass != null ? services.isReady(serviceClass, name) : false;
					final String serviceClassName = serviceClass.getSimpleName() + "." + name;

					final Tag row  = table.block("tr");

					row.block("td").text(serviceClassName);

					if (running) {

						row.block("td").block("button").attr(new Type("button"), new OnClick("window.location.href='" + prefixLocation(ConfigServletLocation) + "?restart=" + serviceClassName + "';")).text("Restart");

						if ("HttpService.default".equals(serviceClassName)) {

							row.block("td");

						} else {

							row.block("td").block("button").attr(new Type("button"), new OnClick("window.location.href='" + prefixLocation(ConfigServletLocation) + "?stop=" + serviceClassName + "';")).text("Stop");
						}

						row.block("td");

					} else {

						row.block("td");
						row.block("td");
						row.block("td").block("button").attr(new Type("button"), new OnClick("window.location.href='" + prefixLocation(ConfigServletLocation) + "?start=" + serviceClassName + "';")).text("Start");
					}
				}
			}

			// stop floating
			container.block("div").attr(new Style("clear: both;"));

			// maintenance tab
			final boolean maintenanceModeActive = Settings.MaintenanceModeEnabled.getValue(false);
			menu.block("li").block("a").id("maintenanceMenu").attr(new Attr("href", "#maintenance")).block("span").text("Maintenance");

			final Tag mContainer     = tabs.block("div").css("tab-content").id("maintenance");

			mContainer.block("h1").text("Maintenance");
			final Tag group  = mContainer.block("div").css("form-group");
			final Tag label  = group.block("label").text("Maintenance Mode is " + (maintenanceModeActive ? "active" : "not active"));
			final Tag button = group.block("td").block("button").attr(new Attr("Type", "button"));

			if (Settings.MaintenanceModeEnabled.getComment() != null) {
				label.attr(new Attr("class", "has-comment"));
				label.attr(new Attr("data-comment", Settings.MaintenanceModeEnabled.getComment()));
			}

			if (maintenanceModeActive) {

				button.attr(new Attr("onclick", "window.location.href='?setMaintenance=false' + location.hash;"));
				button.text("Disable");

			} else {

				button.attr(new Attr("onclick", "window.location.href='?setMaintenance=true' + location.hash;"));
				button.text("Enable");
			}

			// stop floating
			mContainer.block("div").attr(new Style("clear: both;"));

			// buttons
			final Tag buttons = form.block("div").css("buttons");

			buttons.block("button").attr(new Type("button")).id("new-entry-button").text("Add entry");
			buttons.block("button").attr(new Type("button")).id("reload-config-button").text("Reload configuration file");
			buttons.empty("input").css("default-action").attr(new Type("submit"), new Value("Save to structr.conf"));
		}

		// update active section so we can restore it when redirecting
		form.empty("input").attr(new Type("hidden"), new Name("active_section")).id("active_section");

		return doc;
	}

	private Document createLoginDocument(final HttpServletRequest request, final PrintWriter writer) {

		final Document doc = new Document(writer);
		final Tag body     = setupDocument(request, doc).css("login");
		final Tag loginBox = body.block("div").id("login").css("dialog").attr(new Style("display: block; margin: auto; margin-top: 200px;"));

		loginBox.block("svg").attr(new Attr("title", "Structr Logo")).css("logo-login").block("use").attr(new Attr("xlink:href", "#structr-logo"));
		loginBox.block("p").text("Welcome to the " + TITLE + ". Please log in with the <b>super-user</b> password which can be found in your structr.conf.");

		final Tag form     = loginBox.block("form").attr(new Attr("action", prefixLocation(ConfigServletLocation)), new Attr("method", "post"));
		final Tag table    = form.block("table");
		final Tag row1     = table.block("tr");

		row1.block("td").block("label").attr(new Attr("for", "passwordField")).text("Password:");
		row1.block("td").empty("input").attr(new Attr("autofocus", "true")).id("passwordField").attr(new Type("password"), new Name("password"));

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

		final String applicationRootPath = Settings.applicationRootPath.getValue();

		head.block("title").text(TITLE);
		head.empty("meta").attr(new Attr("http-equiv", "Content-Type"), new Attr("content", "text/html;charset=utf-8"));
		head.empty("meta").attr(new Name("viewport"), new Attr("content", "width=1024, user-scalable=yes"));
		head.empty("link").attr(new Rel("stylesheet"), new Href(applicationRootPath + "/structr/css/lib/jquery-ui-1.10.3.custom.min.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href(applicationRootPath + "/structr/css/main.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href(applicationRootPath + "/structr/css/sprites.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href(applicationRootPath + "/structr/css/config.css"));
		head.empty("link").attr(new Rel("icon"), new Href(applicationRootPath + "/favicon.ico"), new Type("image/x-icon"));
		head.block("script").attr(new Src(applicationRootPath + "/structr/js/lib/jquery-3.3.1.min.js"));
		head.block("script").attr(new Src(applicationRootPath + "/structr/js/icons.js"));
		head.block("script").attr(new Src(applicationRootPath + "/structr/js/config.js"));

		final Tag body = doc.block("body");
		final Tag header = body.block("div").id("header");

		header.block("svg").attr(new Attr("title", "Structr Logo")).css("logo").block("use").attr(new Attr("xlink:href", "#structr-logo"));
		final Tag links = header.block("div").id("menu").css("menu").block("ul");

		if (isAuthenticated(request)) {

			final Tag form = links.block("li").block("form").attr(new Attr("action", prefixLocation(ConfigServletLocation)), new Attr("method", "post"), new Style("display: none")).id("logout-form");

			form.empty("input").attr(new Type("hidden"), new Name("action"), new Value("logout"));
			links.block("a").text("Logout").attr(new Style("cursor: pointer"), new OnClick("$('#logout-form').submit();"));
		}

		body.text("<svg style=\"display: none\">" +
				"<symbol id=\"structr-logo\" viewBox=\"0 0 345.12 92.83\" stroke=\"currentColor\" fill=\"currentColor\">" +
				"<g id=\"Ebene_2\" data-name=\"Ebene 2\"><g id=\"Ebene_1-2\" data-name=\"Ebene 1\"><path d=\"M15,33.71a4,4,0,0,0-4,4v4.87c0,1.63,0,4,3.62,5.88l19.75,11c10.25,5.25,11,9.5,11,14v7.12c0,10.25-8.37,12.25-12.87,12.25H1.62A1.25,1.25,0,0,1,.37,91.58V84.46a1.26,1.26,0,0,1,1.25-1.25H30.37a3.92,3.92,0,0,0,4-3.88V75.08c0-1.62,0-4.5-3.5-6L11,58.21C.75,53.58,0,48.08,0,44.21V35.33c0-7.62,6.37-11.25,12.75-11.25H41.12a1.26,1.26,0,0,1,1.25,1.25v7.13a1.25,1.25,0,0,1-1.25,1.25Z\"/><path d=\"M71.75,32.83V79.08a4.08,4.08,0,0,0,4.12,4.13H88.75A1.25,1.25,0,0,1,90,84.46v7.12a1.26,1.26,0,0,1-1.25,1.25H73.87a13.5,13.5,0,0,1-13.5-13.5V4.58a1.73,1.73,0,0,1,1.25-1.75L70.5.08c1-.37,1.25.63,1.25,1.25V23.21h15.5a1.25,1.25,0,0,1,1.25,1.25v7.12a1.26,1.26,0,0,1-1.25,1.25Z\"/><path d=\"M119,34.33a3,3,0,0,0-2.75,3l-.13,54.25a1.25,1.25,0,0,1-1.25,1.25H106a1.25,1.25,0,0,1-1.25-1.25l-.13-53.75c0-6.87,5-13.75,13.75-13.75h17.5a1.26,1.26,0,0,1,1.25,1.25v7.75a1.25,1.25,0,0,1-1.25,1.25Z\"/><path d=\"M181.62,83.21a4.17,4.17,0,0,0,4.13-4.13V25.33A1.25,1.25,0,0,1,187,24.08h8.87a1.26,1.26,0,0,1,1.25,1.25V79.08c0,6.88-5,13.75-13.75,13.75H161c-8.75,0-13.75-6.87-13.75-13.75V25.33a1.25,1.25,0,0,1,1.25-1.25h8.75a1.26,1.26,0,0,1,1.25,1.25V79.08a4.16,4.16,0,0,0,4.12,4.13Z\"/><path d=\"M245.62,92.83H227a13.66,13.66,0,0,1-13.63-13.62V37.71A13.41,13.41,0,0,1,227,24.08h27.62a1.26,1.26,0,0,1,1.25,1.25v7.13a1.25,1.25,0,0,1-1.25,1.25h-26a4,4,0,0,0-4,4V79.33a3.82,3.82,0,0,0,3.88,3.88h26.12a1.25,1.25,0,0,1,1.25,1.25v7.12a1.25,1.25,0,0,1-1.25,1.25Z\"/><path d=\"M279.74,32.83V79.08a4.08,4.08,0,0,0,4.13,4.13h12.87A1.25,1.25,0,0,1,298,84.46v7.12a1.25,1.25,0,0,1-1.25,1.25H281.87a13.5,13.5,0,0,1-13.5-13.5V4.58a1.72,1.72,0,0,1,1.25-1.75L278.49.08c1-.37,1.25.63,1.25,1.25V23.21h15.5a1.25,1.25,0,0,1,1.25,1.25v7.12a1.25,1.25,0,0,1-1.25,1.25Z\"/><path d=\"M327,34.33a3.05,3.05,0,0,0-2.75,3l-.12,54.25a1.26,1.26,0,0,1-1.25,1.25H314a1.25,1.25,0,0,1-1.25-1.25l-.12-53.75c0-6.87,5-13.75,13.75-13.75h17.5a1.26,1.26,0,0,1,1.25,1.25v7.75a1.26,1.26,0,0,1-1.25,1.25Z\"/></g></g>" +
				"</symbol>" +
				"<symbol id=\"interface_delete_circle\" viewBox=\"0 0 10 10\"  stroke=\"currentColor\" fill=\"currentColor\">" +
				"<g><path d=\"M5,0a5,5,0,1,0,5,5A5.006,5.006,0,0,0,5,0ZM7.28,6.22A.75.75,0,1,1,6.22,7.28L5,6.061,3.78,7.28A.75.75,0,0,1,2.72,6.22L3.939,5,2.72,3.78A.75.75,0,0,1,3.78,2.72L5,3.939,6.22,2.72A.75.75,0,0,1,7.28,3.78L6.061,5Z\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"0\"></path></g>" +
				"</symbol>" +
				"<symbol id=\"waiting-spinner\" viewBox=\"0 0 50 50\" style=\"fill: var(--structr-green)\">\n" +
				"<g transform=\"rotate  (0 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.9166666666666666s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate (30 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.8333333333333334s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate (60 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.75s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate (90 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.6666666666666666s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate(120 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.5833333333333334s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate(150 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.5s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate(180 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.4166666666666667s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate(210 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.3333333333333333s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate(240 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.25s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate(270 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.16666666666666666s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate(300 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"-0.08333333333333333s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"<g transform=\"rotate(330 25 25)\"><rect x=\"22\" y=\"0\" rx=\"3\" ry=\"6\" width=\"6\" height=\"12\"><animate attributeName=\"opacity\" values=\"1;0\" keyTimes=\"0;1\" dur=\"1s\" begin=\"0s\" repeatCount=\"indefinite\"></animate></rect></g>\n" +
				"</symbol>" +
				"</svg>");

		return body;
	}

	private boolean isAuthenticated(final HttpServletRequest request) {

		if (!Settings.SetupWizardCompleted.getValue()) {
			return true;
		}

		final HttpSession session = request.getSession();
		if (session != null) {

			final String sessionId = session.getId();
			if (sessionId != null) {

				return sessions.contains(sessionId);

			} else {

				logger.warn("Cannot check HTTP session without session ID, ignoring.");
			}

		} else {

			logger.warn("Cannot check HTTP request, no session.");
		}

		return false;
	}

	private void welcomeTab(final Tag menu, final Tag tabs) {

		final ManageDatabasesCommand cmd   = Services.getInstance().command(null, ManageDatabasesCommand.class);
		final boolean databaseIsConfigured = !cmd.getConnections().isEmpty();
		final boolean passwordIsSet        = StringUtils.isNotBlank(Settings.SuperUserPassword.getValue());
		final Style fgGreen                = new Style("color: #81ce25;");
		final Style bgGreen                = new Style("background-color: #81ce25; color: #fff; border: 1px solid rgba(0,0,0,.125);");
		final String id                    = "welcome";

		menu.block("li").css("active").block("a").id(id + "Menu").attr(new Attr("href", "#" + id)).block("span").text("Start").css("active");

		final Tag container = tabs.block("div").css("tab-content").id(id).attr(new Style("display: block;"));
		final Tag body      = header(container, "Initial Configuration");

		body.block("p").text("This is the first startup in configuration-only mode.");
		body.block("p").text("To start the server and access the user interface, the following actions must be performed:");

		final Tag list  = body.block("ol");
		final Tag item1 = list.block("li").text("Set a <b>superuser</b> password");
		final Tag item2 = list.block("li").text("Configure a <b>database connection</b>");

		if (passwordIsSet) {
			item1.block("span").text(" &#x2714;").attr(fgGreen);
		}

		if (databaseIsConfigured) {
			item2.block("span").text(" &#x2714;").attr(fgGreen);
		}

		if (!passwordIsSet) {

			body.block("h3").text("Superuser password");

			final Tag pwd = body.block("p");
			pwd.empty("input").attr(new Name("superuser.password")).attr(new Type("password")).attr(new Attr("size", 40));
			pwd.empty("input").attr(new Type("submit")).attr(new Attr("value", "Save")).attr(bgGreen);

		} else {

			body.block("h3").text("Next step: ");

			if (databaseIsConfigured) {

				body.block("p").css("steps").block("button").attr(new Type("button")).text("Manage database connections").attr(new OnClick("$('#databasesMenu').click();"));

			} else {

				body.block("p").css("steps").block("button").attr(new Type("button")).text("Configure a database connection").attr(new OnClick("window.location.href='" + prefixLocation(ConfigServletLocation) + "#databases'; $('#databasesMenu').click();"));
			}

		}
	}

	private void databasesTab(final Tag menu, final Tag tabs) {

		final ManageDatabasesCommand cmd           = Services.getInstance().command(null, ManageDatabasesCommand.class);
		final List<DatabaseConnection> connections = cmd.getConnections();
		final String id                            = "databases";

		menu.block("li").block("a").id(id + "Menu").attr(new Attr("href", "#" + id)).block("span").text("Database Connections");

		final Tag container = tabs.block("div").css("tab-content").id(id);
		final Tag body      = header(container, "Database Connections");

		if (connections.isEmpty()) {

			body.block("p").text("There are currently no database connections configured. To use Structr, you have the following options:");

			final Tag div = body.block("div");

			final Tag leftDiv  = div.block("div").css("inline-block");
			leftDiv.block("button").css("default-action").attr(new Type("button")).text("Create new database connection").attr(new OnClick("$('.new-connection.collapsed').removeClass('collapsed')"));
			leftDiv.block("p").text("Configure Structr to connect to a running database.");

			final Tag rightDiv = div.block("div").css("inline-block");
			rightDiv.block("button").attr(new Type("button")).text("Start in demo mode").attr(new OnClick("window.location.href='" + prefixLocation(ConfigServletLocation) + "?finish';"));
			rightDiv.block("p").text("Start Structr in demo mode. Please note that in this mode any data will be lost when stopping the server.");

		} else {

			boolean hasActiveConnection = connections.stream().map(DatabaseConnection::isActive).reduce(false, (t, u) -> t || u);
			if (!hasActiveConnection) {

				body.block("p").text("There is currently no active database connection.");
			}
		}

		// database connections
		for (final DatabaseConnection connection : connections) {

			connection.render(body, prefixLocation(AdminBackendLocation));
		}

		// new connection form should appear below existing connections
		//body.block("div").attr(new Attr("style", "clear: both;"));

		//body.block("h2").text("Add connection");

		final Tag div = body.block("div").css("connection app-tile new-connection collapsed");

		//div.block("h4").text("Add database connection");

		final Tag name = div.block("p");
		name.block("label").text("Name");
		name.add(new InputField(name, "text", "name-structr-new-connection", "", "Enter a connection name"));

		final Tag driver = div.block("p");
		driver.block("label").text("Driver");
		driver.add(new SelectField(driver, "driver-structr-new-connection").addOption("Neo4j", "org.structr.bolt.BoltDatabaseService").addOption("Memgraph DB (experimental)", "org.structr.memgraph.MemgraphDatabaseService"));

		final Tag url = div.block("p");
		url.block("label").text("Connection URL").css("has-comment").attr(new Attr("data-comment", DatabaseConnection.INFO_TEXT_URL));
		url.add(new InputField(url, "text", "url-structr-new-connection", "", "Enter URL"));

		final Tag databaseName = div.block("p");
		databaseName.block("label").text("Database Name").css("has-comment").attr(new Attr("data-comment", DatabaseConnection.INFO_TEXT_DATABASENAME));
		databaseName.add(new InputField(databaseName, "text", "database-structr-new-connection", "", "Enter Database Name"));

		final Tag user = div.block("p");
		user.block("label").text("Username");
		user.add(new InputField(user, "text", "username-structr-new-connection", "", "Enter username"));

		final Tag pass = div.block("p");
		pass.block("label").text("Password");
		pass.add(new InputField(pass, "password", "password-structr-new-connection", "", "Enter password"));

		if (connections.isEmpty()) {

			// allow user to prevent connecting immediately
			final Tag checkbox = div.block("p");
			final Tag label    = checkbox.block("label");
			label.empty("input").attr(new Attr("type", "checkbox"), new Attr("id", "connect-checkbox"), new Attr("checked", "checked"));
			label.block("span").text("Connect immediately");
		}

		final Tag buttons = div.block("p").css("buttons");
		buttons.block("button").attr(new Attr("type", "button")).text("Set Neo4j defaults").attr(new Attr("onclick", "setNeo4jDefaults(this);"));
		buttons.block("button").css("default-action").attr(new Attr("type", "button")).text("Add connection").attr(new Attr("onclick", "addConnection(this);"));

		div.block("div").id("status-structr-new-connection").css("warning warning-message hidden");
	}

	private Tag header(final Tag container, final String title) {

		final Tag div       = container.block("div");
		final Tag main      = div.block("div").css("config-group");

		main.block("h1").text(title);

		// stop floating
		container.block("div").attr(new Style("clear: both;"));

		return main;
	}

	private void authenticateSession(final HttpServletRequest request) {

		final HttpSession session = request.getSession();
		if (session != null) {

			final String sessionId = session.getId();
			if (sessionId != null) {

				sessions.add(sessionId);

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

				sessions.remove(sessionId);

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