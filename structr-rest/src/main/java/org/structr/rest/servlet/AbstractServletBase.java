/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import static org.structr.core.GraphObject.logger;
import org.structr.api.config.Settings;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractServletBase extends HttpServlet {

	protected static final List<String> customResponseHeaders = new LinkedList<>();

	private void addCustomRequestHeaders () {
		final String customResponseHeadersString = Settings.HtmlCustomResponseHeaders.getValue();
		if (StringUtils.isNotBlank(customResponseHeadersString)) {

			customResponseHeaders.addAll(Arrays.asList(customResponseHeadersString.split("[ ,]+")));
		}
	}

	protected void setCustomResponseHeaders(final HttpServletResponse response) {
		addCustomRequestHeaders();

		for (final String header : customResponseHeaders) {

			final String[] keyValuePair = header.split("[ :]+");
			response.setHeader(keyValuePair[0], keyValuePair[1]);

			logger.debug("Set custom response header: {} {}", new Object[]{keyValuePair[0], keyValuePair[1]});
		}
	}

}
