/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.validator;

import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.core.property.PropertyKey;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertyNotFoundToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;

/**
 * A validator that normalizes the given value and ensures it is an
 * existing entity of given type.
 *
 * @author Christian Morgner
 */
public class TypeAndExactNameValidator implements PropertyValidator<String> {

	private Class type = null;

	public TypeAndExactNameValidator(Class type) {
		this.type = type;
	}

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<String> key, String value, ErrorBuffer errorBuffer) {

		if(key == null) {
			return false;
		}

		if (!type.isAssignableFrom(object.getClass())) {
			
			// types are different
			return true;
		}

		if(StringUtils.isBlank(value)) {
			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));
			return false;
		}

		// FIXME: search should be case-sensitive!

		// just check for existance
		try {
			Result nodes = StructrApp.getInstance(securityContext).nodeQuery(type).andName(value).getResult();
			if(nodes != null && !nodes.isEmpty()) {

				return true;

			} else {

				errorBuffer.add(object.getType(), new PropertyNotFoundToken(key, value));
				return false;
			}

		} catch(FrameworkException fex ) {
			// handle error
		}

		return false;
	}
	
	@Override
	public boolean requiresSynchronization() {
		return true;
	}
}
