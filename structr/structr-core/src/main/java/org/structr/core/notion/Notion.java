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



package org.structr.core.notion;

import org.structr.common.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
public abstract class Notion<T> {

	private static final Logger logger = Logger.getLogger(Notion.class.getName());
	
	protected DeserializationStrategy<T, GraphObject> deserializationStrategy = null;
	protected String idProperty                                               = null;
	protected SecurityContext securityContext                                 = null;
	protected SerializationStrategy<GraphObject, T> serializationStrategy     = null;
	protected Class<GraphObject> type                                         = null;

	//~--- constructors ---------------------------------------------------

	public Notion(SerializationStrategy serializationStrategy, DeserializationStrategy deserializationStrategy) {

		this.serializationStrategy   = serializationStrategy;
		this.deserializationStrategy = deserializationStrategy;
	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Returns the property key that will be used to de-serialize objects
	 * with this notion, or null if this notion can not deserialize objects
	 * with a single key.
	 *
	 * @return the primary key property
	 */
	public abstract PropertyKey getPrimaryPropertyKey();

	public Adapter<GraphObject, T> getAdapterForGetter(final SecurityContext securityContext) {

		this.securityContext = securityContext;

		return new Adapter<GraphObject, T>() {

			@Override
			public T adapt(GraphObject s) throws FrameworkException {
				return serializationStrategy.serialize(securityContext, type, s);
			}
		};
	}

	public Adapter<T, GraphObject> getAdapterForSetter(final SecurityContext securityContext) {

		return new Adapter<T, GraphObject>() {

			@Override
			public GraphObject adapt(T s) throws FrameworkException {
				return deserializationStrategy.deserialize(securityContext, type, s);
			}
		};
	}

	public Adapter<Collection<GraphObject>, Collection<T>> getCollectionAdapterForGetter(final SecurityContext securityContext) {

		return new Adapter<Collection<GraphObject>, Collection<T>>() {

			@Override
			public Collection<T> adapt(Collection<GraphObject> s) throws FrameworkException {

				List<T> list = new LinkedList<T>();

				for (GraphObject o : s) {

					list.add(serializationStrategy.serialize(securityContext, type, o));

				}

				return list;
			}
		};
	}

	public Adapter<Collection<T>, Collection<GraphObject>> getCollectionAdapterForSetter(final SecurityContext securityContext) {

		return new Adapter<Collection<T>, Collection<GraphObject>>() {

			@Override
			public Collection<GraphObject> adapt(Collection<T> s) throws FrameworkException {

				List<GraphObject> list = new LinkedList<GraphObject>();
				for (T t : s) {

					list.add(deserializationStrategy.deserialize(securityContext, type, t));

				}

				return list;
			}
		};
	}

	//~--- set methods ----------------------------------------------------

	public void setType(Class<GraphObject> type) {
		this.type = type;
	}

	public void setIdProperty(String idProperty) {
		this.idProperty = idProperty;
	}
	
	public static <S, T> List<T> convertList(List<S> source, Adapter<S, T> adapter) {
		
		List<T> result = new LinkedList<T>();
		for(S s : source) {
	
			try {
				result.add(adapter.adapt(s));
				
			} catch(FrameworkException fex) {
				logger.log(Level.WARNING, "Error in iterable adapter", fex);
			}
	}
		
		return result;
	}
}
