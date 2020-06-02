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
package org.structr.rest.resource;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
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
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;

/**
 *
 *
 */
public class RuntimeEventLogResource extends Resource {

	public enum UriPart {
		_runtimeEventLog
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;

		return (UriPart._runtimeEventLog.name().equals(part));
	}

	@Override public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		final Predicate<RuntimeEvent> predicate = getPredicate();
		final List<GraphObject> resultList      = RuntimeEventLog.getEvents(predicate).stream().map(e -> e.toGraphObject()).collect(Collectors.toList());

		return new PagingIterable(resultList, pageSize, page);
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("POST not allowed on " + getResourceSignature());
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		throw new IllegalPathException(getResourceSignature() + " has no subresources");
	}

	@Override
	public String getUriPart() {
		return getResourceSignature();
	}

	@Override
	public Class getEntityClass() {
		return null;
	}

	@Override
	public String getResourceSignature() {
		return UriPart._runtimeEventLog.name();
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {
		return true;
	}

	// ----- private methods -----
	private Predicate<RuntimeEvent> getPredicate() {

		if (securityContext != null && securityContext.getRequest() != null) {

			final HttpServletRequest request = securityContext.getRequest();
			if (request.getParameter("type") != null) {

				final Set<String> filter = new LinkedHashSet<>(split(request.getParameter("type")));

				return new Predicate<>() {

					@Override
					public boolean accept(final RuntimeEvent value) {
						return filter.contains(value.getType());
					}
				};
			}
		}

		return new Predicate<>() {

			@Override
			public boolean accept(final RuntimeEvent value) {
				return true;
			}
		};
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
}
