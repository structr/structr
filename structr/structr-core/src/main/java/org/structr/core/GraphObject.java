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

import java.util.Date;
import java.util.Map;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 * A common base class for {@see AbstractNode} and {@see AbstractRelationship}.
 *
 * @author Christian Morgner
 */
public interface GraphObject {

	// ----- methods common to both types -----
	/**
	 * Returns the database ID of this graph object.
	 * 
	 * @return the database ID
	 */
	public long getId();
	
	/**
	 * Returns the UUID of this graph object.
	 * 
	 * @return the UUID
	 */
	public String getUuid();
	
	/**
	 * Returns the type of this graph object.
	 * 
	 * @return the type
	 */
	public String getType();

	/**
	 * Returns the property set for the given view as an Iterable.
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	public Iterable<String> getPropertyKeys(String propertyView);
	
	/**
	 * Sets the property with the given key to the given value.
	 * 
	 * @param key the property key to set
	 * @param value the value to set
	 * @throws FrameworkException 
	 */
	public void setProperty(final String key, Object value) throws FrameworkException;

	/**
	 * Sets the property with the given key to the given value.
	 * 
	 * @param key the property key to set
	 * @param value the value to set
	 * @throws FrameworkException 
	 */
	public void setProperty(final PropertyKey key, Object value) throws FrameworkException;
	

	/**
	 * Returns the (converted, validated, transformed, etc.) property for the given
	 * property key.
	 * 
	 * @param propertyKey the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
	public Object getProperty(final String key);


	/**
	 * Returns the (converted, validated, transformed, etc.) property for the given
	 * property key.
	 * 
	 * @param propertyKey the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
	public Object getProperty(final PropertyKey propertyKey);

	/**
	 * Returns the property value for the given key as a String object.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a String object
	 */
	public String getStringProperty(final String key);

	/**
	 * Returns the property value for the given key as a String object.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a String object
	 */
	public String getStringProperty(final PropertyKey propertyKey);

	/**
	 * Returns the property value for the given key as an Integer object.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as an Integer object
	 */
	public Integer getIntProperty(final String key);

	/**
	 * Returns the property value for the given key as an Integer object.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as an Integer object
	 */
	public Integer getIntProperty(final PropertyKey propertyKey);

	/**
	 * Returns the property value for the given key as a Long object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Long object
	 */
	public Long getLongProperty(final String key);

	/**
	 * Returns the property value for the given key as a Long object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Long object
	 */
	public Long getLongProperty(final PropertyKey propertyKey);

	/**
	 * Returns the property value for the given key as a Date object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Date object
	 */
	public Date getDateProperty(final String key);

	/**
	 * Returns the property value for the given key as a Date object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Date object
	 */
        public Date getDateProperty(final PropertyKey key);

	/**
	 * Returns the property value for the given key as a Boolean object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Boolean object
	 */
	public boolean getBooleanProperty(final String key) throws FrameworkException ;

	/**
	 * Returns the property value for the given key as a Boolean object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Boolean object
	 */
        public boolean getBooleanProperty(final PropertyKey key) throws FrameworkException ;

	/**
	 * Returns the property value for the given key as a Double object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Double object
	 */
	public Double getDoubleProperty(final String key) throws FrameworkException ;

	/**
	 * Returns the property value for the given key as a Double object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Double object
	 */
        public Double getDoubleProperty(final PropertyKey key) throws FrameworkException ;

	/**
	 * Returns the property value for the given key as a Comparable
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Comparable
	 */
	public Comparable getComparableProperty(final PropertyKey key) throws FrameworkException;

	/**
	 * Returns the property value for the given key that will be used
	 * for indexing.
	 * 
	 * @param key the key to index the value for
	 * @return the property value for indexing
	 */
	public Object getPropertyForIndexing(final String key);

	/**
	 * Returns the property value for the given key as a Comparable
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Comparable
	 */
	public Comparable getComparableProperty(final String key) throws FrameworkException;

	/**
	 * Removes the property value for the given key from this graph object.
	 * 
	 * @param key the key to remove the value for
	 * @throws FrameworkException 
	 */
	public void removeProperty(final String key) throws FrameworkException;
	
	/**
	 * Returns the default sort key for this entitiy.
	 * 
	 * @return the default sort key
	 */
	public PropertyKey getDefaultSortKey();

	/**
	 * Returns the default sort order for this entity.
	 * 
	 * @return the default sort order
	 */
	public String getDefaultSortOrder();

	/**
	 * Unlock all read-only properties in this entitiy for a single <code>setProperty</code>
	 * call.
	 */
	public void unlockReadOnlyPropertiesOnce();
	
	// ----- callback methods -----
	/**
	 * Called when an entity of this type is created in the database. This method can cause
	 * the underlying transaction to be rolled back in case of an error, either by throwing
	 * an exception, or by returning false.
	 * 
	 * @param securityContext the context in which the creation takes place
	 * @param errorBuffer the error buffer to put error tokens into
	 * @return true if the transaction can go on, false if an error occurred
	 * @throws FrameworkException 
	 */
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException;

	/**
	 * Called when an entity of this type is modified. This method can cause the underlying
	 * transaction to be rolled back in case of an error, either by throwing an exception,
	 * or by returning false.
	 * 
	 * @param securityContext the context in which the modification takes place
	 * @param errorBuffer the error buffer to put error tokens into
	 * @return true if the transaction can go on, false if an error occurred
	 * @throws FrameworkException 
	 */
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException;

	/**
	 * Called when an entity of this type is deleted. This method can cause the underlying
	 * transaction to be rolled back in case of an error, either by throwing an exception,
	 * or by returning false.
	 * 
	 * @param securityContext the context in which the deletion takes place
	 * @param errorBuffer the error buffer to put error tokens into
	 * @return true if the transaction can go on, false if an error occurred
	 * @throws FrameworkException 
	 */
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, Map<String, Object> properties) throws FrameworkException;

	/**
	 * Called when an entity was successfully created. Please note that this method
	 * will run in its own toplevel transaction and can NOT cause the creation
	 * transaction to be rolled back.
	 * 
	 * @param securityContext the context in which the creation took place
	 */
	public void afterCreation(SecurityContext securityContext);

	/**
	 * Called when an entity was successfully modified. Please note that this method
	 * will run in its own toplevel transaction and can NOT cause the modification
	 * transaction to be rolled back.
	 * 
	 * @param securityContext the context in which the modification took place
	 */
	public void afterModification(SecurityContext securityContext);

	/**
	 * Called when an entity was successfully deleted. Please note that this method
	 * will run in its own toplevel transaction and can NOT cause the deletion
	 * transaction to be rolled back.
	 * 
	 * @param securityContext the context in which the deletion took place
	 */
	public void afterDeletion(SecurityContext securityContext);
	
	/**
	 * Called when the owner of this entity was successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent
	 * the owner modification.
	 * 
	 * @param securityContext the context in which the owner modification took place
	 */
	public void ownerModified(SecurityContext securityContext);
	
	/**
	 * Called when the permissions of this entity were successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent the
	 * permission modification.
	 * 
	 * @param securityContext the context in which the permission modification took place
	 */
	public void securityModified(SecurityContext securityContext);
	
	/**
	 * Called when the location of this entity was successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent the
	 * permission modification.
	 * 
	 * @param securityContext the context in which the location modification took place
	 */
	public void locationModified(SecurityContext securityContext);
	
	/**
	 * Called when a non-local modification occurred in the neighbourhood of this node.
	 * 
	 * @param securityContext 
	 */
	public void propagatedModification(SecurityContext securityContext);
}
