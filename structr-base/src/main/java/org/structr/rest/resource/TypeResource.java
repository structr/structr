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

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SchemaNode;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.schema.SchemaHelper;

/**
 * A resource that matches all keywords that might be entity types.
 */
public class TypeResource extends ExactMatchEndpoint {

	public TypeResource() {
		super(RESTParameter.forPattern("type", SchemaNode.schemaNodeNamePattern, true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		final String typeName = call.get("type");
		if (typeName != null) {

			// note: this check is carried out with SuperUser permissions!
			final App app = StructrApp.getInstance();

			// test if resource class exists
			final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
			if (entityClass != null) {

				if (AbstractRelationship.class.isAssignableFrom(entityClass)) {

					return new CollectionResourceHandler(call, entityClass, typeName, false);

				} else {

					return new CollectionResourceHandler(call, entityClass, typeName, true);
				}
			}
		}

		// only return a handler if there is actually a type with the requested name
		return null;
	}
}
