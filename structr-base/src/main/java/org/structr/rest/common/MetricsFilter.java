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
package org.structr.rest.common;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.structr.api.config.Settings;
import org.structr.rest.servlet.MetricsServlet;
import java.io.IOException;

/**
 */
public class MetricsFilter implements jakarta.servlet.Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

		// not for us..
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {

			chain.doFilter(request, response);
			return;
		}

		final HttpServletRequest req   = (HttpServletRequest)request;
		final HttpServletResponse resp = (HttpServletResponse)response;
		final long t0                  = System.currentTimeMillis();

        	try {

			chain.doFilter(request, response);

		} finally {

			final double duration    = (System.currentTimeMillis() - t0) / 1000.0;
			final StringBuilder buf  = new StringBuilder();
			final String method      = req.getMethod();
			final String servletPath = req.getServletPath();
			final int status         = resp.getStatus();


			buf.append(servletPath);

			String path = req.getPathInfo();
			if (path != null) {

				// replace UUIDs with placeholder string to avoid polluting the stats
				path = path.replaceAll(Settings.getValidUUIDRegexString(), "<uuid>");

				buf.append(path);
			}

			MetricsServlet.HTTP_REQUEST_COUNTER.labels(method, buf.toString(), Integer.toString(status)).inc();
			MetricsServlet.HTTP_REQUEST_TIMER.labels(method, buf.toString()).observe(duration);
		}
	}

	@Override
	public void destroy() {
	}
}
