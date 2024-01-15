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
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.app.StructrApp;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.schema.SchemaHelper;

/**
 *
 */
public class InstanceMethodResource extends AbstractTypeIdLowercaseNameResource {

	@Override
	public RESTCallHandler handleTypeIdName(final RESTCall call, final String typeName, final String uuid, final String name) throws FrameworkException {

		final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
		if (entityClass != null) {

			// we fetch the entity here, but only to find out the runtime type, and we don't throw errors here!
			final GraphObject entity = StructrApp.getInstance().get(entityClass, uuid);
			if (entity != null) {

				// use actual type of entity returned to support inheritance
				final AbstractMethod method = Methods.resolveMethod(entity.getClass(), name);
				if (method != null && !method.isPrivate()) {

					return new InstanceMethodResourceHandler(call, entityClass, typeName, uuid, method);
				}
			}
		}

		return null;
	}

}
