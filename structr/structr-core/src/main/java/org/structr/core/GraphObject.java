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

import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.BooleanProperty;
import java.util.Date;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
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

	public static final Property<String>  uuid                        = new StringProperty("uuid").systemProperty().writeOnce();
	public static final Property<String>  type                        = new StringProperty("type").writeOnce();

	public static final Property<Date>    createdDate                 = new ISO8601DateProperty("createdDate").systemProperty().writeOnce();
	public static final Property<Date>    lastModifiedDate            = new ISO8601DateProperty("lastModifiedDate").systemProperty().readOnly();
	public static final Property<Boolean> visibleToPublicUsers        = new BooleanProperty("visibleToPublicUsers");
	public static final Property<Boolean> visibleToAuthenticatedUsers = new BooleanProperty("visibleToAuthenticatedUsers");
	public static final Property<Date>    visibilityStartDate         = new ISO8601DateProperty("visibilityStartDate");
	public static final Property<Date>    visibilityEndDate           = new ISO8601DateProperty("visibilityEndDate");
	
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
	 * Returns the underlying property container for this graph object.
	 * 
	 * @return 
	 */
	public PropertyContainer getPropertyContainer();
	
	/**
	 * Returns the property set for the given view as an Iterable.
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	public Iterable<PropertyKey> getPropertyKeys(String propertyView);
	
	/**
	 * Sets the property with the given key to the given value.
	 * 
	 * @param key the property key to set
	 * @param value the value to set
	 * @throws FrameworkException 
	 */
	public <T> void setProperty(final PropertyKey<T> key, T value) throws FrameworkException;
	
	/**
	 * Returns the (converted, validated, transformed, etc.) property for the given
	 * property key.
	 * 
	 * @param propertyKey the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
	public <T> T getProperty(final PropertyKey<T> propertyKey);
	
	/**
	 * Returns the property value for the given key as an Integer object.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as an Integer object
	 */
	public Integer getIntProperty(final PropertyKey<Integer> propertyKey);

	/**
	 * Returns the property value for the given key as a Long object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Long object
	 */
	public Long getLongProperty(final PropertyKey<Long> propertyKey);

	/**
	 * Returns the property value for the given key as a Date object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Date object
	 */
        public Date getDateProperty(final PropertyKey<Date> key);

	/**
	 * Returns the property value for the given key as a Boolean object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Boolean object
	 */
        public boolean getBooleanProperty(final PropertyKey<Boolean> key) throws FrameworkException ;

	/**
	 * Returns the property value for the given key as a Double object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Double object
	 */
        public Double getDoubleProperty(final PropertyKey<Double> key) throws FrameworkException ;

	/**
	 * Returns the property value for the given key as a Comparable
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Comparable
	 */
	public Comparable getComparableProperty(final PropertyKey<? extends Comparable> key) throws FrameworkException;

	/**
	 * Returns the property value for the given key that will be used
	 * for indexing.
	 * 
	 * @param key the key to index the value for
	 * @return the property value for indexing
	 */
	public Object getPropertyForIndexing(final PropertyKey key);

	/**
	 * Removes the property value for the given key from this graph object.
	 * 
	 * @param key the key to remove the value for
	 * @throws FrameworkException 
	 */
	public void removeProperty(final PropertyKey key) throws FrameworkException;
	
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
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException;

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
