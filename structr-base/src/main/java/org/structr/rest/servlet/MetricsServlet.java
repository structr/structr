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
package org.structr.rest.servlet;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class MetricsServlet extends AbstractDataServlet {
	private final io.prometheus.client.servlet.jakarta.exporter.MetricsServlet servlet;

	public static final Counter HTTP_REQUEST_COUNTER = Counter.build("structr_http_requests_total", "Total number of HTTP requests.").labelNames("method", "path", "status").create().register();
	public static final Histogram HTTP_REQUEST_TIMER = Histogram.build("structr_http_request_duration_seconds", "Duration of HTTP requests.").labelNames("method", "path").exponentialBuckets(0.05, 2.0, 10).create().register();

	private static final Logger logger = LoggerFactory.getLogger(MetricsServlet.class);

	protected final Set<String> whitelist    = new LinkedHashSet<>();
	protected String previousWhitelist       = "";

	public MetricsServlet() {

		servlet = new io.prometheus.client.servlet.jakarta.exporter.MetricsServlet();
		DefaultExports.initialize();
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final String remoteAddress = request.getRemoteAddr();
		if (remoteAddress != null) {

			final Set<String> wl = getWhitelistAddresses();
			if (wl.contains(remoteAddress)) {

				servlet.service(request, response);

				return;
			}
		}

		logger.warn("Access to metrics endpoint denied for remote address {}: not in whitelist. If you want to allow access, edit structr.conf and include {} in metricsservlet.whitelist.", remoteAddress, remoteAddress);

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

	@Override
	public String getModuleName() {
		return "rest";
	}

	// ----- protected methods -----
	protected synchronized Set<String> getWhitelistAddresses() {

		final String whitelistSource = Settings.MetricsServletWhitelist.getValue();
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
