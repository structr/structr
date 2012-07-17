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

package org.structr.core.converter;

import org.structr.core.PropertyConverter;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author Christian Morgner
 */
public class RawPropertyConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		if(value != null) {

			Object v = value.get(securityContext);
			if(v instanceof String) {

				String key = (String)v;

				try {
					if(currentObject instanceof AbstractNode) {
						return ((AbstractNode)currentObject).getNode().getProperty(key);
					} else
					if(currentObject instanceof AbstractRelationship) {
						return ((AbstractRelationship)currentObject).getRelationship().getProperty(key);
					}

				} catch(Throwable t) {

				}
			}
		}

		return source;
	}
}
