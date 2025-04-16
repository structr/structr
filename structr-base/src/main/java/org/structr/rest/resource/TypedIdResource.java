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
package org.structr.rest.resource;


import jakarta.servlet.http.HttpServletResponse;
import org.structr.api.config.Settings;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.IllegalMethodException;

import java.util.*;

/**
 * Represents a type-constrained ID match. A TypedIdResource will always
 * result in a single element.
 *
 *
 */
public class TypedIdResource extends ExactMatchEndpoint {

	public TypedIdResource() {

		super(
			RESTParameter.forPattern("type", SchemaNode.schemaNodeNamePattern, true),
			RESTParameter.forPattern("uuid", Settings.getValidUUIDRegexStringForURLParts(), false)
		);
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		String typeName   = call.get("type");
		final String uuid = call.get("uuid");

		if (typeName != null && uuid != null) {

			// test if type exists
			final Traits traits = Traits.of(typeName);
			if (traits != null) {

				return new EntityResourceHandler(call, typeName, uuid);
			}
		}

		// only return a handler if there is actually a type with the requested name
		return null;
	}

	private class EntityResourceHandler extends RESTCallHandler {

		private String typeName    = null;
		private String uuid        = null;

		public EntityResourceHandler(final RESTCall call, final String typeName, final String uuid) {

			super(call);

			this.typeName    = typeName;
			this.uuid        = uuid;
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			return new PagingIterable<>(getURL(), Arrays.asList(getEntity(securityContext, typeName, uuid)));
		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("POST not allowed on " + typeName + " entity resource", getAllowedHttpMethodsForOptionsCall());
		}

		@Override
		public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {
			return genericPut(securityContext, propertySet);
		}

		@Override
		public RestMethodResult doPatch(final SecurityContext securityContext, final List<Map<String, Object>> propertySets) throws FrameworkException {

			// flatten input (not sure if good, but existing behaviour..)
			final Map<String, Object> flattenedInputs = new HashMap<>();

			for (Map<String, Object> propertySet : propertySets) {

				flattenedInputs.putAll(propertySet);
			}

			// redirect to doPut
			return doPut(securityContext, flattenedInputs);
		}

		@Override
		public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {

			final App app = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx(true, true, false)) {

				final GraphObject obj = getEntity(securityContext, typeName, uuid);

				if (obj.isNode()) {

					final NodeInterface node = (NodeInterface)obj;

					if (!node.isGranted(Permission.delete, securityContext)) {

						return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

					} else {

						app.delete(node);

					}

				} else {

					app.delete((RelationshipInterface) obj);
				}

				tx.success();
			}

			return new RestMethodResult(HttpServletResponse.SC_OK);
		}

		@Override
		public String getTypeName(final SecurityContext securityContext) {
			return typeName;
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public String getResourceSignature() {

			String signature = call.get("type");

			// append requested view to resource signature
			if (!isDefaultView()) {

				signature += "/_" + CaseHelper.toUpperCamelCase(requestedView);
			}

			return signature;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("DELETE", "GET", "OPTIONS", "PUT");
		}
	}
}
