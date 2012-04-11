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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;

/**
 * Combines a serialization strategy and a deserialization strategy
 * to form a notion of an object. A notion in this context is a
 * viewer-specific selection of properties that can be configured
 * separately for each entity via {@see EntityContext}. You can
 * for example configure the User entity to return only its name when
 * referenced from a Folder entity, but to return the whole object
 * when referenced from a Group entity.
 *
 *
 * @author Christian Morgner
 */
public abstract class Notion {

	protected DeserializationStrategy deserializationStrategy = null;
	protected SerializationStrategy serializationStrategy = null;
	protected SecurityContext securityContext = null;
	protected String idProperty = null;
	protected Class type = null;

	/**
	 * Returns the property key that will be used to de-serialize objects
	 * with this notion, or null if this notion can not deserialize objects
	 * with a single key.
	 * 
	 * @return the primary key property
	 */
	public abstract PropertyKey getPrimaryPropertyKey();
	
	public Notion(SerializationStrategy serializationStrategy, DeserializationStrategy deserializationStrategy) {
		this.serializationStrategy = serializationStrategy;
		this.deserializationStrategy = deserializationStrategy;
	}
	
	public Adapter<GraphObject, Object> getAdapterForGetter(final SecurityContext securityContext) {
		this.securityContext = securityContext;
		return new Adapter<GraphObject, Object>() {
			@Override
			public Object adapt(GraphObject s) throws FrameworkException {
				return serializationStrategy.serialize(securityContext, type, s);
			}
		};
	}

	public Adapter<Object, GraphObject> getAdapterForSetter(final SecurityContext securityContext) {
		return new Adapter<Object, GraphObject>() {
			@Override
			public GraphObject adapt(Object s) throws FrameworkException {
				return deserializationStrategy.deserialize(securityContext, type, s);
			}
		};
	}

	public Adapter<Collection<GraphObject>, Object> getCollectionAdapterForGetter(final SecurityContext securityContext) {
		return new Adapter<Collection<GraphObject>, Object>() {
			@Override
			public Object adapt(Collection<GraphObject> s) throws FrameworkException {
				
				List list = new LinkedList();
				if(s instanceof Iterable) {
					Iterable<GraphObject> iterable = (Iterable)s;
					for(GraphObject o : iterable) {
						list.add(serializationStrategy.serialize(securityContext, type, o));
					}
				}

				return list;
			}
		};
	}

	public Adapter<Object, Collection<GraphObject>> getCollectionAdapterForSetter(final SecurityContext securityContext) {
		return new Adapter<Object, Collection<GraphObject>>() {
			@Override
			public Collection<GraphObject> adapt(Object s) throws FrameworkException {
				
				List<GraphObject> list = new LinkedList<GraphObject>();
				if(s instanceof Iterable) {
					Iterable iterable = (Iterable)s;
					for(Object o : iterable) {
						list.add(deserializationStrategy.deserialize(securityContext, type, o));
					}
				}

				return list;
			}
		};
	}

	public void setType(Class type) {
		this.type = type;
	}

	public void setIdProperty(String idProperty) {
		this.idProperty = idProperty;
	}
}
