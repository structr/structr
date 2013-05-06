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
package org.structr.core;

import org.structr.core.property.PropertyKey;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.SecurityContext;

/**
 * A validator that can be used to do validation checks on node properties.
 *
 * @author Christian Morgner
 */
public interface PropertyValidator<T> {

	/**
	 * Indicates whether the given value is valid for the given property
	 * key and parameter.
	 *
	 * @param securityContext
	 * @param key
	 * @param value
	 * @param parameter
	 * @return
	 */
	public abstract boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<T> key, T value, ErrorBuffer errorBuffer);
}
