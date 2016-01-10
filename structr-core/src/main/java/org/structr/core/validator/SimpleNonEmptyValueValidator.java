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
import org.structr.core.property.PropertyKey;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 * A simple validator that checks for non-empty values.
 *
 *
 */
public class SimpleNonEmptyValueValidator implements PropertyValidator<String> {

	private Class type = null;

	public SimpleNonEmptyValueValidator(Class type) {
		this.type = type;
	}

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<String> key, String value, ErrorBuffer errorBuffer) {

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

		if(value != null && value.length() > 0) {
			return true;
		}

		errorBuffer.add(new EmptyPropertyToken(object.getType(), key));
		return false;
	}

	@Override
	public boolean requiresSynchronization() {
		return false;
	}
}
