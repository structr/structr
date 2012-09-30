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

	// ----- common to both types -----
	public long getId();
	public String getUuid();
	public String getType();

	public Iterable<String> getPropertyKeys(String propertyView);
	public void setProperty(final String key, Object value) throws FrameworkException;
	public void setProperty(final PropertyKey key, Object value) throws FrameworkException;
	public Object getProperty(final String key);
        public Object getProperty(final PropertyKey propertyKey);
	public String getStringProperty(final String key);
	public String getStringProperty(final PropertyKey propertyKey);
	public Integer getIntProperty(final String key);
	public Integer getIntProperty(final PropertyKey propertyKey);
	public Long getLongProperty(final String key);
	public Long getLongProperty(final PropertyKey propertyKey);
	public Date getDateProperty(final String key);
        public Date getDateProperty(final PropertyKey key);
	public boolean getBooleanProperty(final String key) throws FrameworkException ;
        public boolean getBooleanProperty(final PropertyKey key) throws FrameworkException ;
	public Double getDoubleProperty(final String key) throws FrameworkException ;
        public Double getDoubleProperty(final PropertyKey key) throws FrameworkException ;
	public Comparable getComparableProperty(final PropertyKey key) throws FrameworkException;
	public Comparable getComparableProperty(final String key) throws FrameworkException;
	public void removeProperty(final String key) throws FrameworkException;

	public PropertyKey getDefaultSortKey();
	public String getDefaultSortOrder();

	public void unlockReadOnlyPropertiesOnce();
	
	// callback methods
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException;
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException;
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, Map<String, Object> properties) throws FrameworkException;

	public void afterCreation(SecurityContext securityContext);
	public void afterModification(SecurityContext securityContext);
	public void afterDeletion(SecurityContext securityContext);
	
	public void ownerModified(SecurityContext securityContext);
	public void securityModified(SecurityContext securityContext);
	public void locationModified(SecurityContext securityContext);
}
