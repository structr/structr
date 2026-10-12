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
import org.structr.common.helper.ServerLogReader;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.GetAvailableServerLogsFunction;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.StringProperty;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.NotAllowedException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Documentation(name="Server log endpoint", type=ConceptType.RestEndpoint, shortDescription="HTTP endpoint that returns the server log, for admin users only. URL path is /_serverLog, with the optional request parameters lines (default 50), level (minimum level), filter (only entries containing this text), since (the offset of a previous response, to read only what was logged after it) and logFile (one of availableLogFiles in the response, the current log first and the rotated ones newest first; since cannot be used with a compressed one).", parent="System endpoints")
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
			final List<String> logFiles      = newestFirst(GetAvailableServerLogsFunction.getListOfServerlogFileNames(true));
			final String requestedFile       = request != null ? request.getParameter("logFile") : null;
			final String filter              = request != null ? request.getParameter("filter") : null;
			final String levelValue          = request != null ? request.getParameter("level") : null;
			final ServerLogReader.Level level = ServerLogReader.parseLevel(levelValue);
			final Long since                 = getSince(request);
			final int lines                  = getLines(request);

			if (requestedFile != null && !logFiles.contains(requestedFile)) {

				throw new FrameworkException(422, "Unknown log file " + requestedFile + ", available are " + logFiles);
			}

			if (StringUtils.isNotBlank(levelValue) && level == null) {

				throw new FrameworkException(422, "Invalid value for level: " + levelValue + ", expected one of " + List.of(ServerLogReader.Level.values()));
			}

			final String logFile        = requestedFile != null ? requestedFile : (logFiles.isEmpty() ? null : logFiles.getFirst());
			final GraphObjectMap result = new GraphObjectMap();
			ServerLogReader.Result log  = new ServerLogReader.Result(List.of(), 0L, false);

			if (logFile != null) {

				try {

					final Path path = Path.of(logFile);

					if (ServerLogReader.isCompressed(path)) {

						if (since != null) {

							throw new FrameworkException(422, "since cannot be used with " + logFile + ", a compressed log has no offsets");
						}

						log = ServerLogReader.tailCompressed(path, lines, level, filter);

					} else {

						log = since != null ? ServerLogReader.since(path, since, lines, level, filter) : ServerLogReader.tail(path, lines, level, filter);
					}

				} catch (IOException ex) {

					throw new FrameworkException(500, "Unable to read " + logFile + ": " + ex.getMessage());
				}
			}

			result.setProperty(new StringProperty("logFile"),            logFile);
			result.setProperty(new GenericProperty("availableLogFiles"), logFiles);
			result.setProperty(new GenericProperty("lines"),             log.lines());
			result.setProperty(new LongProperty("offset"),               log.offset() >= 0 ? log.offset() : null);
			result.setProperty(new BooleanProperty("rotated"),           log.rotated());

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
		private List<String> newestFirst(final List<String> logFiles) {

			// sorted by name, the current log comes first and the rotated ones follow by date, oldest first
			if (logFiles.size() < 2) {

				return logFiles;
			}

			final List<String> sorted = new ArrayList<>(logFiles.subList(1, logFiles.size()));

			Collections.reverse(sorted);
			sorted.addFirst(logFiles.getFirst());

			return sorted;
		}

		private Long getSince(final HttpServletRequest request) throws FrameworkException {

			final String value = request != null ? request.getParameter("since") : null;
			if (StringUtils.isBlank(value)) {

				return null;
			}

			try {

				final long since = Long.parseLong(value.trim());
				if (since >= 0) {

					return since;
				}

			} catch (NumberFormatException ignored) {}

			throw new FrameworkException(422, "Invalid value for since: " + value + ", expected the offset of a previous response");
		}

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
