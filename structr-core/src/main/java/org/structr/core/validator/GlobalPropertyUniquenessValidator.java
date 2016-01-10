/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.validator;

import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.UniqueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.entity.AbstractNode;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.core.app.StructrApp;

//~--- classes ----------------------------------------------------------------

/**
 * A validator that ensures global uniqueness of a given property value,
 * regardless of the entity's type.
 *
 *
 */
public class GlobalPropertyUniquenessValidator<T> implements PropertyValidator<T> {

	private static final Logger logger = Logger.getLogger(GlobalPropertyUniquenessValidator.class.getName());

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<T> key, T value, ErrorBuffer errorBuffer) {

		// FIXME: changed isEmpty to != null
		if (value == null) {

			errorBuffer.add(new EmptyPropertyToken(object.getType(), key));

			return false;

		}

		if (key != null && value != null) {

			String id			= null;
			GraphObject existingNode	= null;

			try {

				// UUID is globally unique
				if (key.equals(GraphObject.id)) {

					//return true; // trust uniqueness
					existingNode = StructrApp.getInstance().getNodeById(value.toString());

				} else {

					existingNode = StructrApp.getInstance().nodeQuery(AbstractNode.class).and(key, value).getFirst();
				}

			} catch (FrameworkException fex) {

				// handle error
			}

			if (existingNode != null) {

				GraphObject foundNode = existingNode;
				if (foundNode.getId() != object.getId()) {

					id = foundNode.getUuid();

					errorBuffer.add(new UniqueToken(object.getType(), key, id));

					return false;
				}
			}
		}

		return true;

	}

	@Override
	public boolean requiresSynchronization() {
		return true;
	}
}
