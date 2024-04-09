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


import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.schema.SchemaHelper;

import java.util.*;
import org.structr.core.entity.SchemaNode;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 *
 */
public class SchemaTypeResource extends ExactMatchEndpoint {

	public enum UriPart {
		_schema
	}

	public SchemaTypeResource() {
		super(
			RESTParameter.forStaticString(UriPart._schema.name()),
			RESTParameter.forPattern("type", SchemaNode.schemaNodeNamePattern)
		);
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		final String typeName = call.get("type");
		if (typeName != null) {

			final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
			if (entityClass != null) {

				if (call.isDefaultView()) {

					return new SchemaTypeResourceHandler(call, entityClass, typeName, null);

				} else {

					return new SchemaTypeResourceHandler(call, entityClass, typeName, call.getViewName());
				}
			}
		}

		return null;
	}

	// ----- public static methods -----
	public static ResultStream getSchemaTypeResult(final SecurityContext securityContext, final String rawType, final Class type, final String propertyView) throws FrameworkException {
		return new PagingIterable<>("getSchemaTypeResult(" + rawType + ")", SchemaHelper.getSchemaTypeInfo(securityContext, rawType, type, propertyView));
	}

	private class SchemaTypeResourceHandler extends RESTCallHandler {

		private Class entityClass = null;
		private String typeName   = null;
		private String viewName   = null;

		public SchemaTypeResourceHandler(final RESTCall call, final Class entityClass, final String typeName, final String viewName) {

			super(call);

			this.entityClass = entityClass;
			this.typeName    = typeName;
			this.viewName    = viewName;
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			return SchemaTypeResource.getSchemaTypeResult(securityContext, typeName, entityClass, viewName);
		}

		@Override
		public Class getEntityClass(final SecurityContext securityContext) {
			return null;
		}

		@Override
		public boolean isCollection() {
			return true;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("GET", "OPTIONS");
		}
	}

}
