/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.rest.resource.CypherQueryResource;
import org.structr.rest.resource.EntityResolverResource;
import org.structr.rest.resource.EnvResource;
import org.structr.rest.resource.GlobalSchemaMethodsResource;
import org.structr.rest.resource.LogResource;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.rest.resource.MeResource;
import org.structr.rest.resource.PropertyResource;
import org.structr.rest.resource.RelationshipsResource;
import org.structr.rest.resource.RuntimeEventLogResource;
import org.structr.rest.resource.SchemaResource;
import org.structr.rest.resource.SchemaTypeResource;
import org.structr.rest.resource.TypeResource;
import org.structr.rest.resource.TypedIdResource;
import org.structr.rest.resource.UuidResource;
import org.structr.web.resource.LoginResource;
import org.structr.web.resource.LogoutResource;
import org.structr.web.resource.RegistrationResource;
import org.structr.web.resource.ResetPasswordResource;
import org.structr.web.resource.TokenResource;

/**
 *
 */
public class APIEndpoints {

	private static final List<SortedByUsageCount> ENDPOINTS = new LinkedList<>();

	static {

		// initialize static API endpoints
		APIEndpoints.register(new CypherQueryResource());
		APIEndpoints.register(new EntityResolverResource());
		APIEndpoints.register(new EnvResource());
		APIEndpoints.register(new GlobalSchemaMethodsResource());
		APIEndpoints.register(new LogResource());
		APIEndpoints.register(new LoginResource());
		APIEndpoints.register(new LogoutResource());
		APIEndpoints.register(new MaintenanceResource());
		APIEndpoints.register(new MeResource());
		APIEndpoints.register(new PropertyResource());
		APIEndpoints.register(new RegistrationResource());
		APIEndpoints.register(new ResetPasswordResource());
		APIEndpoints.register(new RelationshipsResource());
		APIEndpoints.register(new RuntimeEventLogResource());
		APIEndpoints.register(new SchemaResource());
		APIEndpoints.register(new SchemaTypeResource());
		APIEndpoints.register(new TokenResource());
		APIEndpoints.register(new TypeResource());
		APIEndpoints.register(new TypedIdResource());
		APIEndpoints.register(new UuidResource());
	}

	// ----- public static methods -----
	public static void register(final APIEndpoint endpoint) {
		ENDPOINTS.add(new SortedByUsageCount(endpoint));
	}

	public static APICallHandler resolveAPICallHandler(final SecurityContext securityContext, final HttpServletRequest request) throws FrameworkException {

		final List<String> viewHolder = new LinkedList<>();
		final String path             = APIEndpoints.removeTrailingViewName(request.getPathInfo(), viewHolder);
		final String viewName         = viewHolder.isEmpty() ? null : viewHolder.get(0);

		// endpoints are sorted by frequency of successfully resolved matches
		Collections.sort(ENDPOINTS);

		for (final SortedByUsageCount container : ENDPOINTS) {

			final Matcher matcher = container.matcher(path);
			if (matcher.matches()) {

				final APIEndpoint endpoint   = container.getItem();
				final APICall call           = endpoint.initializeAPICall(matcher, viewName);
				final APICallHandler handler = endpoint.accept(securityContext, call);

				if (handler != null) {

					container.incrementUsageCount();

					return handler;
				}
			}
		}

		throw new FrameworkException(404, "Cannot resolve URL path");
	}

	// ----- private static methods -----
	private static String removeTrailingViewName(final String path, final List<String> propertyView) {

		final Set<String> propertyViews = Services.getInstance().getConfigurationProvider().getPropertyViews();
		final int positionOfLastSlash   = path.lastIndexOf("/") + 1;

		if (positionOfLastSlash > 0) {

			final String lastPathElement = path.substring(positionOfLastSlash);
			if (propertyViews.contains(lastPathElement)) {

				// last path element is a view, save it
				propertyView.add(lastPathElement);

				// ... and return path without it
				return path.substring(0, positionOfLastSlash);
			}
		}

		return path;
	}

	// ----- nested classes -----
	private static class SortedByUsageCount implements Comparable<SortedByUsageCount> {

		private APIEndpoint endpoint = null;
		private long usageCount      = 0;

		public SortedByUsageCount(final APIEndpoint item) {
			this.endpoint = item;
		}

		@Override
		public String toString() {
			return usageCount + ": " + endpoint.toString();
		}

		@Override
		public int hashCode() {
			return endpoint.hashCode();
		}

		@Override
		public boolean equals(final Object other) {
			return endpoint.equals(other);
		}

		@Override
		public int compareTo(final SortedByUsageCount other) {
			return Long.compare(other.usageCount, this.usageCount);
		}

		public void incrementUsageCount() {
			this.usageCount++;
		}

		public APIEndpoint getItem() {
			return endpoint;
		}

		public Matcher matcher(final String path) {
			return endpoint.matcher(path);
		}
	}
}
