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


import org.structr.common.ResultTransformer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaHelper;

import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.common.SecurityContext;
import org.structr.core.entity.SchemaNode;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.parameter.RESTParameter;

/**
 * A resource that matches all keywords that might be entity types.
 */
public class TypeResource extends ExactMatchEndpoint {

	public TypeResource() {
		super(RESTParameter.forPattern("type", SchemaNode.schemaNodeNamePattern));
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {

		final String typeName = call.get("type");
		if (typeName != null) {

			final App app = StructrApp.getInstance(securityContext);

			// check if this resource representes a virtual type
			final String rawType = checkVirtualType(app, typeName);

			// test if resource class exists
			final Class entityClass = SchemaHelper.getEntityClassForRawType(rawType);
			if (entityClass != null) {

				if (AbstractRelationship.class.isAssignableFrom(entityClass)) {

					return new CollectionResourceHandler(securityContext, call, app.relationshipQuery(entityClass), entityClass, rawType, false);

				} else {

					return new CollectionResourceHandler(securityContext, call, app.nodeQuery(entityClass), entityClass, rawType, true);
				}
			}
		}

		// only return a handler if there is actually a type with the requested name
		return null;
	}

	private String checkVirtualType(final App app, final String typeName) throws FrameworkException {

		final Class<? extends AbstractNode> virtualTypeClass = StructrApp.getConfiguration().getNodeEntityClass("VirtualType");
		if (virtualTypeClass != null) {

			final PropertyKey<Integer> positionProperty = StructrApp.key(virtualTypeClass, "position");
			final ResultTransformer virtualType         = (ResultTransformer)app.nodeQuery(virtualTypeClass).andName(typeName).sort(positionProperty).getFirst();

			if (virtualType != null) {

				final String sourceType = virtualType.getSourceType();
				if (sourceType != null) {

					// return source type of virtual type
					return sourceType;

				} else {

					throw new FrameworkException(500, "Invalid virtual type " + typeName + ", missing value for sourceType");
				}
			}
		}

		// just pass through
		return typeName;
	}
}
