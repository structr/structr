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
package org.structr.rest.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.GetAvailableServerLogsFunction;
import org.structr.core.function.ServerLogFunction;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.StringProperty;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.NotAllowedException;

import java.util.List;
import java.util.Set;

@Documentation(name="Server log endpoint", type=ConceptType.RestEndpoint, shortDescription="HTTP endpoint that returns the last lines of the server log, for admin users only. URL path is /_serverLog, with the optional request parameters lines (default 50), filter (only lines containing this text) and logFile (one of availableLogFiles in the response).", parent="System endpoints")
public class ServerLogResource extends ExactMatchEndpoint {

	private static final int DEFAULT_LINES = 50;

	public enum UriPart {

		_serverLog
	}

	public ServerLogResource() {

		super(RESTParameter.forStaticString(UriPart._serverLog.name(), true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		return new ServerLogResourceHandler(call);
	}

	private class ServerLogResourceHandler extends RESTCallHandler {

		public ServerLogResourceHandler(final RESTCall call) {

			super(call);
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			// the log carries everything the instance has been sent, so a grant alone must not open it
			if (securityContext == null || !securityContext.isSuperUser()) {

				throw new NotAllowedException("Access to the server log is restricted to admin users");
			}

			final HttpServletRequest request = securityContext.getRequest();
			final List<String> logFiles      = GetAvailableServerLogsFunction.getListOfServerlogFileNames();
			final String requestedFile       = request != null ? request.getParameter("logFile") : null;
			final String filter              = request != null ? request.getParameter("filter") : null;
			final int lines                  = getLines(request);

			if (requestedFile != null && !logFiles.contains(requestedFile)) {

				throw new FrameworkException(422, "Unknown log file " + requestedFile + ", available are " + logFiles);
			}

			final String logFile = requestedFile != null ? requestedFile : (logFiles.isEmpty() ? null : logFiles.getFirst());
			final String log     = logFile != null ? ServerLogFunction.getServerLog(lines, -1, logFile, filter) : "";
			final GraphObjectMap result = new GraphObjectMap();

			result.setProperty(new StringProperty("logFile"),            logFile);
			result.setProperty(new GenericProperty("availableLogFiles"), logFiles);
			result.setProperty(new GenericProperty("lines"),             log.isEmpty() ? List.of() : List.of(log.split("\n")));

			return new PagingIterable(getURL(), List.of(result));
		}

		@Override
		public String getTypeName(final SecurityContext securityContext) {

			return null;
		}

		@Override
		public boolean isCollection() {

			return false;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {

			return Set.of("GET", "OPTIONS");
		}

		// ----- private methods -----
		private int getLines(final HttpServletRequest request) throws FrameworkException {

			final String value = request != null ? request.getParameter("lines") : null;
			if (StringUtils.isBlank(value)) {

				return DEFAULT_LINES;
			}

			try {

				final int lines = Integer.parseInt(value.trim());
				if (lines > 0) {

					return lines;
				}

			} catch (NumberFormatException ignored) {}

			throw new FrameworkException(422, "Invalid value for lines: " + value + ", expected a positive number");
		}
	}
}
