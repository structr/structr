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
package org.structr.web.servlet;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.QueryHistogram;
import org.structr.docs.Documentation;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * A servlet that implements the /histogram endpoint.
 */
@Documentation(name="HistogramServlet", parent="Servlets", children={ "HistogramServlet Settings" })
public class HistogramServlet extends HealthCheckServlet {

	private static final Logger logger = LoggerFactory.getLogger(HistogramServlet.class);

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=utf-8");

		final String remoteAddress = request.getRemoteAddr();
		if (remoteAddress != null) {

			final Set<String> wl = getWhitelistAddresses();
			if (!wl.contains(remoteAddress)) {

				logger.warn("Access to histogram endpoint denied for remote address {}: not in whitelist. If you want to allow access, edit structr.conf and includ {} in histogramservlet.whitelist.", remoteAddress, remoteAddress);

				response.sendError(HttpServletResponse.SC_FORBIDDEN);

				return;
			}
		}

		try (final Writer writer = response.getWriter()) {

			final String sortKey = request.getParameter("sort");
			final String top     = request.getParameter("top");

			gson.toJson(QueryHistogram.analyze(stringOrDefault(sortKey, "total"), intOrDefault(top, 1000)), writer);

			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("Cache-Control", "max-age=60");

			writer.append("\n");
			writer.flush();
		}

		if (request.getParameter("reset") != null) {

			logger.info("Clearing query histogram data..");

			QueryHistogram.clear();
		}
	}

	// ----- private methods -----
	private int intOrDefault(final String value, int defaultValue) {

		if (value != null) {

			try { return Integer.valueOf(value); } catch (Throwable t) {}
		}

		return defaultValue;
	}

	private String stringOrDefault(final String value, String defaultValue) {

		if (value != null) {

			return value;
		}

		return defaultValue;
	}
}
