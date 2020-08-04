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

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.QueryHistogram;

/**
 * A servlet that implements the /histogram endpoint.
 */
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

				logger.warn("Access to histogram endpoint denied for remote address {}: not in whitelist. If you want to allow access, add {} to histogramservlet.whitelist in structr.conf.");

				response.sendError(HttpServletResponse.SC_FORBIDDEN);

				return;
			}
		}

		try (final Writer writer = response.getWriter()) {

			gson.toJson(QueryHistogram.analyze(), writer);

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
}
