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
package org.structr.core.property;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 * A string property that lets the user create a value
 * when there is no value associated with it yet.
 * 
 *
 */
public abstract class AutoStringProperty extends StringProperty {

	private static final Logger logger = Logger.getLogger(AutoStringProperty.class.getName());
	
	public AutoStringProperty(String name) {
		
		super(name);
		
		// mark this property as being passively indexed
		// so we can be sure it will be activated when
		// a new entity with this property is created
		passivelyIndexed();
	}
	
	public abstract String createValue(GraphObject entity);
	
	@Override
	public void index(GraphObject entity, Object value) {
		
		Object indexValue = value;
		
		if (indexValue == null) {
			
			indexValue = createValue(entity);
			if (indexValue != null) {
				
				try {
					entity.setProperty(this, (String)indexValue);
					
				} catch (FrameworkException fex) {
					
					logger.log(Level.WARNING, "Unable to set value {0} on entity {1}: {2}", new Object[] { indexValue, entity, fex.getMessage() } );
				}
			}
		}
				
		super.index(entity, indexValue);
	}
}
