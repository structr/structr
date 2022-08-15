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
package org.structr.rest.servlet;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MetricsServlet extends AbstractDataServlet {

	public static final Counter HTTP_REQUEST_COUNTER = Counter.build("structr_http_requests_total", "Total number of HTTP requests.").labelNames("method", "path", "status").create().register();
	public static final Histogram HTTP_REQUEST_TIMER = Histogram.build("structr_http_request_duration_seconds", "Duration of HTTP requests.").labelNames("method", "path").exponentialBuckets(0.001, 10.0, 4).create().register();

	private final io.prometheus.client.exporter.MetricsServlet servlet;

	public MetricsServlet() {

		servlet = new io.prometheus.client.exporter.MetricsServlet();
		DefaultExports.initialize();
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		servlet.service(req, res);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		servlet.service(req, resp);
	}

	@Override
	public String getModuleName() {
		return "rest";
	}
}
