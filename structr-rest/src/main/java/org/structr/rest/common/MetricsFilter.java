/*
 * Copyright (C) 2010-2022 Structr GmbH
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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.structr.rest.servlet.MetricsServlet;

/**
 */
public class MetricsFilter implements jakarta.servlet.Filter {

	private static final Pattern UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{32}");

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

			final double duration    = Double.valueOf(System.currentTimeMillis() - t0) / 1000.0;
			final StringBuilder buf  = new StringBuilder();
			final String method      = req.getMethod();
			final String servletPath = req.getServletPath();
			final String path        = req.getPathInfo();
			final int status         = resp.getStatus();

			buf.append(servletPath);

			if (path != null) {

				// replace UUIDs with placeholder string to avoid polluting the stats
				final Matcher matcher = UUID_PATTERN.matcher(path);

				buf.append(matcher.replaceAll("<uuid>"));
			}

			MetricsServlet.HTTP_REQUEST_COUNTER.labels(method, buf.toString(), Integer.toString(status)).inc();
			MetricsServlet.HTTP_REQUEST_TIMER.labels(method, buf.toString()).observe(duration);
		}
	}

	@Override
	public void destroy() {
	}
}
