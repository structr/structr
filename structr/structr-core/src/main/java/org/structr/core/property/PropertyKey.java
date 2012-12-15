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

package org.structr.core.property;

import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchOperator;

/**
 * Base interface for typed property keys.
 *
 * @author Christian Morgner
 */
public interface PropertyKey<T> {
	
	public String jsonName();
	public String dbName();
	
	/**
	 * Returns the desired type name that will be used in the error message if a
	 * wrong type was provided.
	 */
	public String typeName();
	public Class relatedType();
	
	public T defaultValue();
	
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity);
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext);
	
	public void setDeclaringClass(Class<? extends GraphObject> declaringClass);
	public Class<? extends GraphObject> getDeclaringClass();

	public SearchAttribute getSearchAttribute(SearchOperator op, T searchValue, boolean exactMatch);
	public void registerSearchableProperties(Set<PropertyKey> searchableProperties);
	
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter);
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException;

	public void registrationCallback(Class entityType);
	
	/**
	 * Indicates whether this property is a system property or not. If a transaction
	 * contains only modifications AND those modifications affect system properties
	 * only, structr will NOT call afterModification callbacks.
	 * 
	 * @return 
	 */
	public boolean isSystemProperty();
	
	public boolean isReadOnlyProperty();

	public boolean isWriteOnceProperty();

	public boolean isCollection();

}
