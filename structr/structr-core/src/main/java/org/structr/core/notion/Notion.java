/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.core.notion;

import org.structr.common.SecurityContext;
import org.structr.core.Adapter;
import org.structr.core.GraphObject;

/**
 * Combines a serialization strategy and a deserialization strategy
 * to form a notion of an object.
 *
 *
 * @author Christian Morgner
 */
public abstract class Notion {

	protected DeserializationStrategy deserializationStrategy = null;
	protected SerializationStrategy serializationStrategy = null;
	protected SecurityContext securityContext = null;
	protected Class type = null;

	public Notion(SerializationStrategy serializationStrategy, DeserializationStrategy deserializationStrategy) {
		this.serializationStrategy = serializationStrategy;
		this.deserializationStrategy = deserializationStrategy;
	}
	
	public Adapter<GraphObject, Object> getAdapterForGetter(final SecurityContext securityContext) {
		this.securityContext = securityContext;
		return new Adapter<GraphObject, Object>() {
			@Override
			public Object adapt(GraphObject s) {
				return serializationStrategy.serialize(securityContext, type, s);
			}
		};
	}

	public Adapter<Object, GraphObject> getAdapterForSetter(final SecurityContext securityContext) {
		return new Adapter<Object, GraphObject>() {
			@Override
			public GraphObject adapt(Object s) {
				return deserializationStrategy.deserialize(securityContext, type, s);
			}
		};
	}

	public void setType(Class type) {
		this.type = type;
	}
}
