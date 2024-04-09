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
package org.structr.rest.servlet;


import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.rest.common.StatsCallback;

import java.io.IOException;
import java.util.Arrays;

public abstract class AbstractServletBase extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(AbstractServletBase.class.getName());

	protected StatsCallback stats = null;

	public void registerStatsCallback(final StatsCallback stats) {
		this.stats = stats;
	}

	protected void setCustomResponseHeaders(final HttpServletResponse response) {

		if (response != null) {

			final String customResponseHeadersString = Settings.HtmlCustomResponseHeaders.getValue();
			if (StringUtils.isNotBlank(customResponseHeadersString)) {

				for (final String header : Arrays.asList(customResponseHeadersString.split("[,]+"))) {

					final String[] keyValuePair = header.split("[:]+");
					if (keyValuePair != null && keyValuePair.length == 2) {

						response.setHeader(keyValuePair[0].trim(), keyValuePair[1].trim());

						logger.debug("Set custom response header: {} {}", keyValuePair[0].trim(), keyValuePair[1].trim());
					}
				}
			}
		}
	}

	protected void assertInitialized() throws FrameworkException {

		final Services services = Services.getInstance();
		if (!services.isInitialized()) {
			throw new FrameworkException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, services.getUnavailableMessage());
		}
	}

	protected void sendRedirectHeader(final HttpServletResponse response, final String location) throws IOException {

		sendRedirectHeader(response, location, true);
	}

	protected void sendRedirectHeader(final HttpServletResponse response, final String location, final boolean addPrefix) throws IOException {

		final String locationWithSlash     = ((location.startsWith("/") ? "" : "/") + location);
		final String finalRedirectLocation = (addPrefix ? prefixLocation(locationWithSlash) : locationWithSlash);

		response.resetBuffer();
		response.setHeader("Location", finalRedirectLocation);
		response.setStatus(HttpServletResponse.SC_FOUND);
		response.flushBuffer();
	}

	public static String prefixLocation (final String location) {

		return Settings.ApplicationRootPath.getValue() + ((location.startsWith("/") ? "" : "/") + location);
	}
}
