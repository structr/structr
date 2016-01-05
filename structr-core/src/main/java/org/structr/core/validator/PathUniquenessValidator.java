/**
 * Copyright (C) 2010-2015 Structr GmbH
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
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;

//~--- classes ----------------------------------------------------------------

/**
 * A validator that ensures global uniqueness for given entity type.
 *
 *
 */
public class PathUniquenessValidator<T> implements PropertyValidator<T> {

	private Class type = null;

	public PathUniquenessValidator(Class type) {
		this.type = type;
	}

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<T> key, T value, ErrorBuffer errorBuffer) {

		/*
		 * Explanation: the property key fields of an entity are static and final, so for example
		 * the name property exists exactly once, but each type can register a validator for
		 * uniqueness on it. The property has a list of validators that are checked separately,
		 * one for each type, so it can happen that the name property has several different
		 * validators. The below check is there to ensure that only the correct type is used
		 * for validation.
		 */

		if (!type.isAssignableFrom(object.getClass())) {

			// types are different
			return true;
		}

		if (value == null) {

			if (errorBuffer != null) {
				errorBuffer.add(new EmptyPropertyToken(object.getType(), key));
			}

			return false;

		}

		if (key != null) {

			Result<AbstractNode> result = null;
			boolean nodeExists          = false;
			String id                   = null;

			try {

				result = StructrApp.getInstance().nodeQuery(type).and(key, value).getResult();
				nodeExists = !result.isEmpty();

			} catch (FrameworkException fex) {

				fex.printStackTrace();

			}

			if (nodeExists) {

				AbstractNode foundNode = result.get(0);
				if (foundNode.getId() != object.getId()) {

					id = ((AbstractNode) result.get(0)).getUuid();

					if (errorBuffer != null) {
						errorBuffer.add(new UniqueToken(object.getType(), key, id));
					}

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
