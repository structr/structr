/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.entity.SchemaNode;
import org.structr.core.traits.Traits;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;

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
		if (typeName != null && Traits.exists(typeName)) {

			// test if resource class exists
			final Traits traits = Traits.of(typeName);
			if (traits != null) {

				return new CollectionResourceHandler(call, typeName, traits.isNodeType());
			}
		}

		// only return a handler if there is actually a type with the requested name
		return null;
	}
}
