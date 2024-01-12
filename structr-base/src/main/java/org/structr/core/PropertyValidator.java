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
package org.structr.core;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.property.PropertyKey;

/**
 * A validator that can be used to do validation checks on node properties.
 *
 *
 * @param <T>
 */
public interface PropertyValidator<T> {

	/**
	 * Indicates whether the given value is valid for the given property
	 * key and parameter.
	 *
	 * @param securityContext
	 * @param object
	 * @param key
	 * @param value
	 * @param errorBuffer
	 * @return valid
	 */
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<T> key, T value, ErrorBuffer errorBuffer);

	public boolean requiresSynchronization();
}
