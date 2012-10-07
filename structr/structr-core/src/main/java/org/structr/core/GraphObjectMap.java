/*
 *  Copyright (C) 2012 Axel Morgner
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

import java.util.*;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */

public class GraphObjectMap implements GraphObject, Map<String, Object> {

	private Map<String, Object> values = new LinkedHashMap<String, Object>();

	@Override
	public long getId() {
		return -1;
	}

	@Override
	public String getUuid() {
		return getStringProperty(AbstractNode.Key.uuid);
	}

	@Override
	public String getType() {
		return getStringProperty(AbstractNode.Key.uuid);
	}

	@Override
	public Iterable<String> getPropertyKeys(String propertyView) {
		return values.keySet();
	}

	@Override
	public void setProperty(String key, Object value) throws FrameworkException {
		values.put(key, value);
	}

	@Override
	public void setProperty(PropertyKey key, Object value) throws FrameworkException {
		setProperty(key.name(), value);
	}

	@Override
	public Object getProperty(String key) {
		return values.get(key);
	}

	@Override
	public Object getProperty(PropertyKey propertyKey) {
		return getProperty(propertyKey.name());
	}

	@Override
	public String getStringProperty(String key) {
		return (String)getProperty(key);
	}

	@Override
	public String getStringProperty(PropertyKey propertyKey) {
		return (String)getProperty(propertyKey);
	}

	@Override
	public Integer getIntProperty(String key) {
		return (Integer)getProperty(key);
	}

	@Override
	public Integer getIntProperty(PropertyKey propertyKey) {
		return (Integer)getProperty(propertyKey);
	}

	@Override
	public Long getLongProperty(String key) {
		return (Long)getProperty(key);
	}

	@Override
	public Long getLongProperty(PropertyKey propertyKey) {
		return (Long)getProperty(propertyKey);
	}

	@Override
	public Date getDateProperty(String key) {
		return (Date)getProperty(key);
	}

	@Override
	public Date getDateProperty(PropertyKey key) {
		return (Date)getProperty(key);
	}

	@Override
	public boolean getBooleanProperty(String key) throws FrameworkException {
		return (Boolean)getProperty(key);
	}

	@Override
	public boolean getBooleanProperty(PropertyKey key) throws FrameworkException {
		return (Boolean)getProperty(key);
	}

	@Override
	public Double getDoubleProperty(String key) throws FrameworkException {
		return (Double)getProperty(key);
	}

	@Override
	public Double getDoubleProperty(PropertyKey key) throws FrameworkException {
		return (Double)getProperty(key);
	}

	@Override
	public Comparable getComparableProperty(PropertyKey key) throws FrameworkException {
		return (Comparable)getProperty(key);
	}

	@Override
	public Comparable getComparableProperty(String key) throws FrameworkException {
		return (Comparable)getProperty(key);
	}

	@Override
	public void removeProperty(String key) throws FrameworkException {
		values.remove(key);
	}

	@Override
	public PropertyKey getDefaultSortKey() {
		return null;
	}

	@Override
	public String getDefaultSortOrder() {
		return "asc";
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {
	}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, Map<String, Object> properties) throws FrameworkException {
		return true;
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
	}

	@Override
	public void afterDeletion(SecurityContext securityContext) {
	}

	@Override
	public void ownerModified(SecurityContext securityContext) {
	}

	@Override
	public void securityModified(SecurityContext securityContext) {
	}

	@Override
	public void locationModified(SecurityContext securityContext) {
	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {
	}

	// ----- interface map -----
	@Override
	public int size() {
		return values.size();
	}

	@Override
	public boolean isEmpty() {
		return values.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return values.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return values.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return values.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return values.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return values.remove(key);
	}

	@Override
	public void putAll(Map m) {
		values.putAll(m);
	}

	@Override
	public void clear() {
		values.clear();
	}

	@Override
	public Set keySet() {
		return values.keySet();
	}

	@Override
	public Collection values() {
		return values.values();
	}

	@Override
	public Set entrySet() {
		return values.entrySet();
	}

	@Override
	public Object getPropertyForIndexing(String key) {
		return getProperty(key);
	}
}
