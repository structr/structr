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
package org.structr.web.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.Tx;
import org.structr.rest.common.Stats;
import org.structr.rest.service.HttpService;
import org.structr.rest.servlet.AbstractDataServlet;
import org.structr.schema.SchemaService;
import org.structr.web.maintenance.DeployCommand;

import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * A servlet that implements the /health endpoint.
 */
public class HealthCheckServlet extends AbstractDataServlet {

	private static final Logger logger  = LoggerFactory.getLogger(HealthCheckServlet.class);

	protected final Gson gson                = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
	protected final Set<String> whitelist    = new LinkedHashSet<>();
	protected final Map<String, Object> data = new LinkedHashMap<>();
	protected final long updateInterval      = 1000L;
	protected long lastUpdate                = 0L;
	protected String previousWhitelist       = "";
	protected int statusCode                 = -1;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {

				final Authenticator auth = getConfig().getAuthenticator();

				// Ensure CORS settings apply by letting the authenticator examine the request.
				auth.initializeAndExamineRequest(request, response);

				tx.success();
			}

			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/health+json; charset=utf-8");

			final String remoteAddress = request.getRemoteAddr();
			if (remoteAddress != null) {

				if (request.getPathInfo() != null && request.getPathInfo().equals("/ready")) {

					if (DeployCommand.isDeploymentActive() || SchemaService.isCompiling()) {

						response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
					} else {

						response.setStatus(HttpServletResponse.SC_OK);
					}

					return;
				}

				final Set<String> wl = getWhitelistAddresses();
				if (!wl.contains(remoteAddress)) {

					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}
			}

			try (final Writer writer = response.getWriter()) {

				if (System.currentTimeMillis() > lastUpdate + updateInterval) {

					lastUpdate = System.currentTimeMillis();

					synchronized (data) {

						data.clear();

						data.put("version", "1.1");
						data.put("description", "Structr system health status");

						// service layer available?
						if (Services.getInstance().isInitialized()) {

							// status is "pass" or "warn", only pass for now..
							data.put("status", "pass");

							final Map<String, Object> details = new LinkedHashMap<>();
							final ThreadMXBean threadMXBean   = ManagementFactory.getThreadMXBean();
							final Runtime runtime             = Runtime.getRuntime();

							data.put("checks", details);

							embedGroup(details, "memory:utilization",
									embedValue("free memory",  "system", runtime.freeMemory(),  "bytes", "pass"),
									embedValue("max memory",   "system", runtime.maxMemory(),   "bytes", "pass"),
									embedValue("total memory", "system", runtime.totalMemory(), "bytes", "pass")
							);

							embedGroup(details, "cpu:utilization",
									embedValue("load average 1 min", "system", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(), null, "pass")
							);

							embedGroup(details, "uptime",
									embedValue("uptime",  "system", ManagementFactory.getRuntimeMXBean().getUptime(), "ms", "pass")
							);

							embedGroup(details, "threads",
									embedValue("current thread count", "system", threadMXBean.getThreadCount(),       null, "pass"),
									embedValue("peak thread count",    "system", threadMXBean.getPeakThreadCount(),   null, "pass"),
									embedValue("daemon thread count",  "system", threadMXBean.getDaemonThreadCount(), null, "pass")
							);

							final Map<String, Map<String, Integer>> info = Services.getInstance().getDatabaseService().getCachesInfo();
							final Map<String, Integer> nodeCacheInfo     = info.get("nodes");
							final Map<String, Integer> relCacheInfo      = info.get("relationships");

							if (nodeCacheInfo != null) {

								embedGroup(details, "cache:node",
										embedValue("size",  "system", nodeCacheInfo.get("max"),  null, "pass"),
										embedValue("count", "system", nodeCacheInfo.get("size"), null, "pass")
								);
							}

							if (relCacheInfo != null) {

								embedGroup(details, "cache:relationship",
										embedValue("size",  "system", relCacheInfo.get("max"),  null, "pass"),
										embedValue("count", "system", relCacheInfo.get("size"), null, "pass")
								);
							}

							final HttpService httpService = Services.getInstance().getService(HttpService.class, "default");
							if (httpService != null) {

								{
									// html
									final List<Map<String, Object>> measurements = new LinkedList<>();

									details.put("html:responseTime", measurements);

									final Map<String, Stats> stats = httpService.getRequestStats("html");
									if (stats != null) {

										for (final String key : stats.keySet()) {

											final Stats statsData = stats.get(key);

											measurements.add(embedValue(key, "page", statsData.getMinValue(),     "ms", "pass",   "min"));
											measurements.add(embedValue(key, "page", statsData.getMaxValue(),     "ms", "pass",   "max"));
											measurements.add(embedValue(key, "page", statsData.getAverageValue(), "ms", "pass",   "avg"));
											measurements.add(embedValue(key, "page", statsData.getCount(),        null, "pass", "count"));
										}
									}
								}

								{
									// json
									final List<Map<String, Object>> measurements = new LinkedList<>();

									details.put("json:responseTime", measurements);

									final Map<String, Stats> stats = httpService.getRequestStats("json");
									if (stats != null) {

										for (final String key : stats.keySet()) {

											final Stats statsData = stats.get(key);

											measurements.add(embedValue(key, "endpoint", statsData.getMinValue(),     "ms", "pass",   "min"));
											measurements.add(embedValue(key, "endpoint", statsData.getMaxValue(),     "ms", "pass",   "max"));
											measurements.add(embedValue(key, "endpoint", statsData.getAverageValue(), "ms", "pass",   "avg"));
											measurements.add(embedValue(key, "endpoint", statsData.getCount(),        null, "pass", "count"));
										}
									}
								}
							}

							statusCode = HttpServletResponse.SC_OK;

						} else {

							statusCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

							data.put("status", "fail");
						}

					}
				}

				synchronized (data) {
					gson.toJson(data, writer);
				}

				response.setStatus(statusCode);
				response.setHeader("Cache-Control", "max-age=60");

				writer.append("\n");
				writer.flush();
			}

		} catch (FrameworkException ex) {

		}
	}

	@Override
	public String getModuleName() {
		return "rest";
	}

	// ----- protected methods -----
	protected synchronized Set<String> getWhitelistAddresses() {

		final String whitelistSource = Settings.HealthCheckWhitelist.getValue();
		if (!whitelistSource.equals(previousWhitelist)) {

			whitelist.clear();

			for (final String entry : whitelistSource.split(",")) {

				final String trimmed = entry.trim();

				if (StringUtils.isNotBlank(trimmed)) {

					whitelist.add(trimmed);
				}
			}

			// cache contents to detect changes
			previousWhitelist = whitelistSource;
		}

		return whitelist;
	}

	// ----- private methods -----
	private void embedGroup(final Map<String, Object> dest, final String name, final Map<String, Object>... data) {

		final List<Map<String, Object>> list = new LinkedList<>();

		for (final Map<String, Object> entry : data) {

			list.add(entry);
		}

		dest.put(name, list);
	}

	private Map<String, Object> embedValue(final String componentId, final String componentType, final Object value, final String unit, final String status) {
		return embedValue(componentId, componentType, value, unit, status, null);
	}

	private Map<String, Object> embedValue(final String componentId, final String componentType, final Object value, final String unit, final String status, final String valueDescription) {

		final Map<String, Object> valueContainer = new LinkedHashMap<>();

		valueContainer.put("componentType", componentType);
		valueContainer.put("componentId",   componentId);
		valueContainer.put("observedValue", value);

		if (unit != null) {
			valueContainer.put("observedUnit", unit);
		}

		valueContainer.put("status", status);
		valueContainer.put("time", new Date(lastUpdate));

		if (valueDescription != null) {
			valueContainer.put("valueDescription", valueDescription);
		}

		return valueContainer;
	}
}
