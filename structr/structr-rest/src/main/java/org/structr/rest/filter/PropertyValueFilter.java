/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

package org.structr.rest.filter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Predicate;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class PropertyValueFilter<T extends Comparable> extends Filter {

	private static final Logger logger = Logger.getLogger(PropertyValueFilter.class.getName());
	
	private Predicate<T> predicate = null;
	private String propertyKey = null;
	private Value<T> value = null;

	public PropertyValueFilter(String propertyKey, Predicate<T> predicate, Value<T> value) {
		this.propertyKey = propertyKey;
		this.predicate = predicate;
		this.value = value;
	}

	@Override
	public boolean includeInResultSet(SecurityContext securityContext, GraphObject object) {
		
		T t = (T) object.getPropertyForIndexing(propertyKey);
		if (t != null) {
			return predicate.evaluate(securityContext, t, value.get(securityContext));
		} else {
			logger.log(Level.WARNING, "Null property for key {0} on ID {1}", new Object[] { propertyKey, object.getProperty(AbstractNode.Key.uuid.name()) });
		}
		
		return false;
	}
}
