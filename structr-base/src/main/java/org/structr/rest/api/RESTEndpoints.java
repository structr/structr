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
package org.structr.rest.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.rest.resource.CollectionRelationshipsResource;
import org.structr.rest.resource.CypherQueryResource;
import org.structr.rest.resource.EntityResolverResource;
import org.structr.rest.resource.EnvResource;
import org.structr.rest.resource.GlobalSchemaMethodsResource;
import org.structr.rest.resource.InstanceMethodResource;
import org.structr.rest.resource.LogResource;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.rest.resource.MeResource;
import org.structr.rest.resource.PropertyResource;
import org.structr.rest.resource.InstanceRelationshipsResource;
import org.structr.rest.resource.RuntimeEventLogResource;
import org.structr.rest.resource.SchemaResource;
import org.structr.rest.resource.SchemaTypeResource;
import org.structr.rest.resource.StaticMethodResource;
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
public class RESTEndpoints {

	private static final List<SortedByUsageCount> ENDPOINTS = new LinkedList<>();

	static {

		// initialize static API endpoints
		RESTEndpoints.register(new CollectionRelationshipsResource());
		RESTEndpoints.register(new CypherQueryResource());
		RESTEndpoints.register(new EntityResolverResource());
		RESTEndpoints.register(new EnvResource());
		RESTEndpoints.register(new GlobalSchemaMethodsResource());
		RESTEndpoints.register(new InstanceMethodResource());
		RESTEndpoints.register(new InstanceRelationshipsResource());
		RESTEndpoints.register(new LogResource());
		RESTEndpoints.register(new LoginResource());
		RESTEndpoints.register(new LogoutResource());
		RESTEndpoints.register(new MaintenanceResource());
		RESTEndpoints.register(new MeResource());
		RESTEndpoints.register(new PropertyResource());
		RESTEndpoints.register(new RegistrationResource());
		RESTEndpoints.register(new ResetPasswordResource());
		RESTEndpoints.register(new RuntimeEventLogResource());
		RESTEndpoints.register(new SchemaResource());
		RESTEndpoints.register(new SchemaTypeResource());
		RESTEndpoints.register(new StaticMethodResource());
		RESTEndpoints.register(new TokenResource());
		RESTEndpoints.register(new TypeResource());
		RESTEndpoints.register(new TypedIdResource());
		RESTEndpoints.register(new UuidResource());
	}

	// ----- public static methods -----
	public static void register(final RESTEndpoint endpoint) {
		ENDPOINTS.add(new SortedByUsageCount(endpoint));
	}

	public static RESTCallHandler resolveRESTCallHandler(final SecurityContext securityContext, final HttpServletRequest request, final String defaultView) throws FrameworkException {

		final List<String> viewHolder = new LinkedList<>();
		final String path             = RESTEndpoints.removeTrailingViewName(request.getPathInfo(), viewHolder);
		final String viewName         = viewHolder.isEmpty() ? defaultView : viewHolder.get(0);

		// endpoints are sorted by frequency of successfully resolved matches
		Collections.sort(ENDPOINTS);

		for (final SortedByUsageCount container : ENDPOINTS) {

			final Matcher matcher = container.matcher(path);
			if (matcher.matches()) {

				final RESTEndpoint endpoint   = container.getItem();
				final RESTCall call           = endpoint.initializeRESTCall(matcher, viewName);
				final RESTCallHandler handler = endpoint.accept(securityContext, call);

				if (handler != null) {

					handler.setRequestedView(viewName);

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
		final int positionOfLastSlash   = path.lastIndexOf("/");

		if (positionOfLastSlash > -1) {

			final String lastPathElement = path.substring(positionOfLastSlash + 1);
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

		private RESTEndpoint endpoint = null;
		private long usageCount      = 0;

		public SortedByUsageCount(final RESTEndpoint item) {
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

		public RESTEndpoint getItem() {
			return endpoint;
		}

		public Matcher matcher(final String path) {
			return endpoint.matcher(path);
		}
	}
}
