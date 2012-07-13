/*
 *  Copyright (C) 2010-2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core;

import org.structr.common.SecurityContext;

/**
 * A generic converter interface that can be used to convert
 * values from one type to another. Please note that implementations
 * of this interface MUST be able to handle null values.
 *
 * @author Christian Morgner
 */
public abstract class PropertyConverter<S, T> {

	protected SecurityContext securityContext = null;
	protected GraphObject currentObject = null;
	protected boolean rawMode = false;

	/**
	 * Converts from destination type to source type. Caution: source
	 * will be null if there is no value in the database.
	 * 
	 * @param source
	 * @return 
	 */
	public abstract S convertForSetter(T source, Value value);
	
	/**
	 * Converts from source type to destination type. Caution: source
	 * will be null if there is no value in the database.
	 * 
	 * @param source
	 * @return 
	 */
	public abstract T convertForGetter(S source, Value value);

	/**
	 * Convert from source type to Comparable to allow a more
	 * fine-grained control over the sorted results. Override 
	 * this method to modify sorting behaviour of entities.
	 * 
	 * @param source
	 * @param value
	 * @return 
	 */
	public Comparable convertForSorting(S source, Value value) {
		
		T target = convertForGetter(source, value);
		if(target != null) {
			
			if (target instanceof Comparable) {
				
				return (Comparable)target;
			}
			
			// fallback
			return target.toString();
		}
		
		return null;
	}
	
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
	
	public void setCurrentObject(GraphObject currentObject) {
		this.currentObject = currentObject;
	}

	public void setRawMode(boolean rawMode) {
		this.rawMode = rawMode;
	}
	
	public boolean getRawMode() {
		return rawMode;
	}
}
