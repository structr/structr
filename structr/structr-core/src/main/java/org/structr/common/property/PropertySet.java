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
package org.structr.common.property;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 * Refactoring helper.
 * 
 * @author Christian Morgner
 */
public class PropertySet implements Map<PropertyKey, Object> {
	
	private static final Logger logger = Logger.getLogger(PropertySet.class.getName());
	
	protected Map<PropertyKey, Object> properties = new LinkedHashMap<PropertyKey, Object>();

	public PropertySet() {}
	
	public PropertySet(Map<PropertyKey, Object> source) {
		properties.putAll(source);
	}
	
	@Override
	public int size() {
		return properties.size();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return properties.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return properties.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return properties.get(key);
	}

	@Override
	public Object put(PropertyKey key, Object value) {
		return properties.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return properties.remove(key);
	}

	@Override
	public void putAll(Map<? extends PropertyKey, ? extends Object> m) {
		properties.putAll(m);
	}

	@Override
	public void clear() {
		properties.clear();
	}

	@Override
	public Set<PropertyKey> keySet() {
		return properties.keySet();
	}

	@Override
	public Collection<Object> values() {
		return properties.values();
	}

	@Override
	public Set<Entry<PropertyKey, Object>> entrySet() {
		return properties.entrySet();
	}
	
	
	// ----- static methods -----
	public static Map<String, Object> revert(PropertySet source) {
		
		Map<String, Object> dest = new LinkedHashMap<String, Object>();
		for (Entry<PropertyKey, Object> entry : source.entrySet()) {
			
			dest.put(entry.getKey().name(), entry.getValue());
		}
		
		return dest;
	}
	
	public static PropertySet convert(Map<String, Object> source) throws FrameworkException {
		
		Object typeName = source.get(AbstractNode.type.name());
		if (typeName != null) {
			
			Class<? extends GraphObject> type = EntityContext.getEntityClassForRawType(typeName.toString());
			if (type != null) {
				
				return convert(type, source);
				
			} else {
				
				logger.log(Level.WARNING, "No entity type found for raw type {0}", typeName);
			}
			
		} else {
				
			logger.log(Level.WARNING, "No entity type found in source map");
		}
			
		return fallbackPropertyMap(source);
	}
	
	public static PropertySet convert(Class<? extends GraphObject> type, Map<String, Object> source) throws FrameworkException {

		// fallback: AbstractNode
		if (type == null) {
			type = AbstractNode.class;
		}
		
		try {
			return convert(type.newInstance(), source);
			
		} catch(Throwable t) {
			
			logger.log(Level.WARNING, "Unable to convert property map: {0}", t.getMessage());
		}
			
		return fallbackPropertyMap(source);
	}
	
	public static PropertySet convert(GraphObject entity, Map<String, Object> source) throws FrameworkException {
		
		PropertySet resultMap = new PropertySet();
		
		for (Entry<String, Object> entry : source.entrySet()) {
	
			String key   = entry.getKey();
			Object value = entry.getValue();
			
			if (key != null && value != null) {

				PropertyKey propertyKey = entity.getPropertyKeyForName(key);
				Object propertyValue    = propertyKey.fromPrimitive(entity, value);
				
				resultMap.put(propertyKey, propertyValue);
			}
		}
		
		return resultMap;
	}
	
	private static PropertySet fallbackPropertyMap(Map<String, Object> source) {

		logger.log(Level.WARNING, "Using fallback property set conversion without type safety!");
		
		Thread.dumpStack();
		
	
		PropertySet map = new PropertySet();
		for (Entry<String, Object> entry : source.entrySet()) {
	
			String key   = entry.getKey();
			Object value = entry.getValue();
			
			if (key != null && value != null) {
			
				map.put(new Property(key), value);
			}
		}

		return map;
	}
}
