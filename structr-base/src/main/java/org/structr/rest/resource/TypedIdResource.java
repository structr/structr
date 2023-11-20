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
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.schema.SchemaHelper;

import java.util.Arrays;
import java.util.Map;
import org.structr.api.APICall;
import org.structr.api.APICallHandler;
import org.structr.api.APIEndpoint;
import org.structr.api.config.Settings;
import org.structr.api.parameter.APIParameter;
import org.structr.core.entity.SchemaNode;

/**
 * Represents a type-constrained ID match. A TypedIdResource will always
 * result in a single element.
 *
 *
 */
public class TypedIdResource extends APIEndpoint {

	private static final APIParameter typeParameter = APIParameter.forPattern("type", SchemaNode.schemaNodeNamePattern);
	private static final APIParameter uuidParameter = APIParameter.forPattern("uuid", Settings.getValidUUIDRegexString());

	public TypedIdResource() {

		super(
			typeParameter,
			uuidParameter
		);
	}

	@Override
	public APICallHandler accept(final SecurityContext securityContext, final APICall call) throws FrameworkException {

		final String typeName = call.get(typeParameter);
		final String uuid     = call.get(uuidParameter);

		if (typeName != null && uuid != null) {

			// test if resource class exists
			final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
			if (entityClass != null) {

				return new EntityResourceHandler(securityContext, call.getURL(), entityClass, typeName, uuid);
			}
		}

		// only return a handler if there is actually a type with the requested name
		return null;
	}

	private class EntityResourceHandler extends APICallHandler {

		private Class entityClass = null;
		private String typeName   = null;
		private String uuid       = null;

		public EntityResourceHandler(final SecurityContext securityContext, final String url, final Class entityClass, final String typeName, final String uuid) {

			super(securityContext, url);

			this.entityClass = entityClass;
			this.typeName    = typeName;
			this.uuid        = uuid;
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			return new PagingIterable<>(getURL(), Arrays.asList(getEntity(entityClass, typeName, uuid)));
		}

		@Override
		public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("POST not allowed on " + typeName + " entity resource");
		}

		@Override
		public Class getEntityClass() {
			return entityClass;
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public RestMethodResult doDelete() throws FrameworkException {

			final App app = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx(true, true, false)) {

				final GraphObject obj = getEntity(entityClass, typeName, uuid);

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
	}
}
