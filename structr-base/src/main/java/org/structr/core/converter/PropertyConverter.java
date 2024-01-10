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
package org.structr.core.converter;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 * A generic converter interface that can be used to convert
 * values from one type to another. Please note that implementations
 * of this interface MUST be able to handle null values.
 *
 *
 */
public abstract class PropertyConverter<S, T> {

	protected SecurityContext securityContext = null;
	protected GraphObject currentObject       = null;
	protected boolean rawMode                 = false;
	protected Object context                  = null;

	public PropertyConverter(SecurityContext securityContext) {
		this(securityContext, null);
	}

	public PropertyConverter(SecurityContext securityContext, GraphObject currentObject) {
		this.securityContext = securityContext;
		this.currentObject = currentObject;
	}

	/**
	 * Converts from destination type to source type. Caution: source
	 * will be null if there is no value in the database.
	 *
	 * @param source
	 * @return reverted source
	 * @throws org.structr.common.error.FrameworkException
	 */
	public abstract S revert(T source) throws FrameworkException;

	/**
	 * Converts from source type to destination type. Caution: source
	 * will be null if there is no value in the database.
	 *
	 * @param source
	 * @return converted source
	 * @throws org.structr.common.error.FrameworkException
	 */
	public abstract T convert(S source) throws FrameworkException;

	/**
	 * Convert from source type to Comparable to allow a more
	 * fine-grained control over the sorted results. Override
	 * this method to modify sorting behaviour of entities.
	 *
	 * @param source
	 * @return converted source
	 * @throws org.structr.common.error.FrameworkException
	 */
	public Comparable convertForSorting(S source) throws FrameworkException {

		if(source != null) {

			if (source instanceof Comparable) {

				return (Comparable)source;
			}

			// fallback
			return source.toString();
		}

		return null;
	}

	public void setRawMode(boolean rawMode) {
		this.rawMode = rawMode;
	}

	public boolean getRawMode() {
		return rawMode;
	}

	public void setContext(final Object context) {
		this.context = context;
	}
}
