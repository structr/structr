/**
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

import java.util.Arrays;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;

public abstract class AbstractServletBase extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(AbstractServletBase.class.getName());

	protected void setCustomResponseHeaders(final HttpServletResponse response) {

		if (response != null) {

			final String customResponseHeadersString = Settings.HtmlCustomResponseHeaders.getValue();
			if (StringUtils.isNotBlank(customResponseHeadersString)) {

				for (final String header : Arrays.asList(customResponseHeadersString.split("[ ,]+"))) {

					final String[] keyValuePair = header.split("[ :]+");
					if (keyValuePair != null && keyValuePair.length == 2) {

						response.setHeader(keyValuePair[0], keyValuePair[1]);

						logger.debug("Set custom response header: {} {}", keyValuePair[0], keyValuePair[1]);
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
}
