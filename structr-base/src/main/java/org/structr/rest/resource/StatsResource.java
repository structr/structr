/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.common.Stats;
import org.structr.rest.service.HttpService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class StatsResource extends ExactMatchEndpoint {

	public StatsResource() {
		super(RESTParameter.forStaticString("stats", true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {
		return new StatsResourceHandler(call);
	}

	private class StatsResourceHandler extends RESTCallHandler {

		public StatsResourceHandler(final RESTCall call) {
			super(call);
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			final HttpService httpService = Services.getInstance().getServiceImplementation(HttpService.class);
			if (httpService != null) {

				final Map<String, Map<Long, Long>> map = new LinkedHashMap<>();
				final Map<String, Stats> stats         = httpService.getRequestStats("http");
				final HttpServletRequest request       = securityContext.getRequest();
				final Long interval                    = coalesce(longOrNull(request.getParameter("interval")), 60_000L);
				final Long maxCount                    = coalesce(longOrNull(request.getParameter("max")), Long.MAX_VALUE);

				final Stats getStats = stats.get("get");
				if (getStats != null) {

					map.put("GET", getStats.aggregate(interval, maxCount));
				}

				final Stats putStats = stats.get("put");
				if (putStats != null) {

					map.put("PUT", putStats.aggregate(interval, maxCount));
				}

				final Stats patchStats = stats.get("patch");
				if (patchStats != null) {

					map.put("PATCH", patchStats.aggregate(interval, maxCount));
				}

				final Stats postStats = stats.get("post");
				if (postStats != null) {

					map.put("POST", postStats.aggregate(interval, maxCount));
				}

				final Stats deleteStats = stats.get("delete");
				if (deleteStats != null) {

					map.put("DELETE", deleteStats.aggregate(interval, maxCount));
				}

				final Stats headStats = stats.get("head");
				if (headStats != null) {

					map.put("HEAD", headStats.aggregate(interval, maxCount));
				}

				final Stats optionsStats = stats.get("options");
				if (optionsStats != null) {

					map.put("OPTIONS", optionsStats.aggregate(interval, maxCount));
				}

				return new PagingIterable("stats", List.of(map));
			}

			throw new FrameworkException(404, "HttpService not available.");
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

		private <T> T coalesce(final T... values) {

			for (final T value : values) {

				if (value != null) {
					return value;
				}
			}

			return null;
		}

		private Long longOrNull(final String value) {

			if (value == null) {
				return null;
			}

			return Long.valueOf(value);
		}
	}
}
