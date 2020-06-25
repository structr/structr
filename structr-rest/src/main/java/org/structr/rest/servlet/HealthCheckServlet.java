/*
 * Copyright (C) 2010-2020 Structr GmbH
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.core.Services;

/**
 * A servlet that implements the /health endpoint.
 */
public class HealthCheckServlet extends AbstractDataServlet {

	private static final Logger logger  = LoggerFactory.getLogger(HealthCheckServlet.class);

	private final Gson gson             = new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
	private final Set<String> whitelist = new LinkedHashSet<>();
	private String previousWhitelist    = "";

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=utf-8");

		final String remoteAddress = request.getRemoteAddr();
		if (remoteAddress != null) {

			final Set<String> wl = getWhitelistAddresses();
			if (!wl.contains(remoteAddress)) {

				logger.warn("Access to health check endpoint denied for remote address {}: not in whitelist. If you want to allow access, add {} to healthcheckservlet.whitelist in structr.conf.");

				response.sendError(HttpServletResponse.SC_FORBIDDEN);

				return;
			}
		}

		try (final Writer writer = response.getWriter()) {

			final Map<String, Object> data = new LinkedHashMap<>();
			int statusCode                 = -1;

			data.put("version", "1.0");
			data.put("description", "Structr system health status");

			// service layer available?
			if (Services.getInstance().isInitialized()) {

				// status is "pass" or "warn", only pass for now..
				data.put("status", "pass");

				final Map<String, Object> details    = new LinkedHashMap<>();
				data.put("details", details);

				final List<Map<String, Object>> list = new LinkedList<>();
				details.put("memory:utilization", list);

				list.add(embedValue("free memory",  Runtime.getRuntime().freeMemory(),  "1 bytes", "pass"));
				list.add(embedValue("max memory",   Runtime.getRuntime().maxMemory(),   "1 bytes", "pass"));
				list.add(embedValue("total memory", Runtime.getRuntime().totalMemory(), "1 bytes", "pass"));

				statusCode = HttpServletResponse.SC_OK;

			} else {

				statusCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

				data.put("status", "fail");
			}

			gson.toJson(data, writer);

			response.setStatus(statusCode);
			response.setHeader("Cache-Control", "max-age=60");

			writer.append("\n");
			writer.flush();
		}
	}

	@Override
	public String getModuleName() {
		return "rest";
	}

	// ----- private methods -----
	private Map<String, Object> embedValue(final String componentId, final long value, final String unit, final String status) {

		final Map<String, Object> valueContainer = new LinkedHashMap<>();

		valueContainer.put("componentType", "system");
		valueContainer.put("componentId", componentId);
		valueContainer.put("observedValue", value);
		valueContainer.put("observedUnit", unit);
		valueContainer.put("status", status);
		valueContainer.put("time", new Date());

		return valueContainer;
	}

	private synchronized Set<String> getWhitelistAddresses() {

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
}
