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
import org.apache.commons.lang3.StringUtils;
import org.structr.api.AndPredicate;
import org.structr.api.Predicate;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEvent;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObject;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *
 *
 */
public class RuntimeEventLogResource extends ExactMatchEndpoint {

	public enum UriPart {
		_runtimeEventLog
	}

	public RuntimeEventLogResource() {
		super(RESTParameter.forStaticString(UriPart._runtimeEventLog.name(), true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {
		return new RuntimeEventLogResourceHandler(call);
	}

	private class RuntimeEventLogResourceHandler extends RESTCallHandler {

		public RuntimeEventLogResourceHandler(final RESTCall call) {
			super(call);
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			final Predicate<RuntimeEvent> predicate = getPredicate(securityContext);
			final List<GraphObject> resultList      = RuntimeEventLog.getEvents(predicate).stream().map(e -> e.toGraphObject()).collect(Collectors.toList());

			return new PagingIterable(getURL(), resultList, pageSize, page);
		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			final Consumer<RuntimeEvent> visitor    = getVisitor(propertySet);
			final Predicate<RuntimeEvent> predicate = getPredicate(securityContext);

			if (visitor != null) {

				RuntimeEventLog.getEvents(predicate).stream().forEach(visitor);
			}

			return new RestMethodResult(200);
		}

		@Override
		public String getTypeName(final SecurityContext securityContext) {
			return "RuntimeEvent";
		}

		@Override
		public boolean isCollection() {
			return true;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("GET", "OPTIONS", "POST");
		}

		// ----- private methods -----
		private Predicate<RuntimeEvent> getPredicate(final SecurityContext securityContext) {

			final AndPredicate<RuntimeEvent> root = new AndPredicate<>();

			if (securityContext != null && securityContext.getRequest() != null) {

				final HttpServletRequest request   = securityContext.getRequest();

				if (request.getParameter(RuntimeEvent.ID_PROPERTY) != null) {

					final long id = Long.parseLong(request.getParameter(RuntimeEvent.ID_PROPERTY));

					root.addPredicate(new Predicate<>() {

						@Override
						public boolean accept(final RuntimeEvent value) {
							return id == value.getId();
						}
					});
				}

				if (request.getParameter(RuntimeEvent.SEEN_PROPERTY) != null) {

					final boolean seen = Boolean.parseBoolean(request.getParameter(RuntimeEvent.SEEN_PROPERTY));

					root.addPredicate(new Predicate<>() {

						@Override
						public boolean accept(final RuntimeEvent value) {
							return seen == value.getSeen();
						}
					});
				}

				if (request.getParameter(RuntimeEvent.TYPE_PROPERTY) != null) {

					final Set<String> filter = new LinkedHashSet<>(split(request.getParameter(RuntimeEvent.TYPE_PROPERTY)));

					root.addPredicate(new Predicate<>() {

						@Override
						public boolean accept(final RuntimeEvent value) {
							return filter.contains(value.getType());
						}
					});
				}
			}

			return root;
		}

		private List<String> split(final String source) {

			final List<String> parts = new LinkedList<>();

			if (source != null) {

				for (final String part : source.split("[,]+")) {

					final String trimmed = part.trim();
					if (StringUtils.isNotBlank(trimmed)) {

						parts.add(part);
					}
				}
			}

			return parts;
		}

		private Consumer<RuntimeEvent> getVisitor(final Map<String, Object> properties) {

			if ("acknowledge".equals(properties.get("action"))) {
				return t -> t.acknowledge();
			}

			return null;
		}
	}
}
