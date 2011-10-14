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

package org.structr.core.resource;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.core.entity.DirectedRelationship;

/**
 * A global context for system-wide mappings between nodes and
 * relationships.
 *
 * @author Christian Morgner
 */
public class ResourceContext {

	private static final Map<String, Map<String, DirectedRelationship>> globalTypeMap = new LinkedHashMap<String, Map<String, DirectedRelationship>>();
	private static final Map<Class, Map<PropertyView, Set<String>>> globalPropertyKeyMap = new LinkedHashMap<Class, Map<PropertyView, Set<String>>>();

	
	public static DirectedRelationship getRelation(String sourceType, String destType) {
		return getTypeMap(sourceType).get(destType);
	}

	public static DirectedRelationship getRelation(Class sourceType, Class destType) {
		return getTypeMap(convertName(sourceType)).get(convertName(destType));
	}

	public static void registerRelation(Class sourceType, Class destType, RelationshipType relType, Direction direction) {
		registerRelation(convertName(sourceType), convertName(destType), relType, direction);
	}

	public static void registerRelation(String sourceType, String destType, RelationshipType relType, Direction direction) {

		Map<String, DirectedRelationship> typeMap = getTypeMap(sourceType);
		typeMap.put(destType, new DirectedRelationship(relType, direction));
	}

	public static void registerPropertySet(Class type, PropertyView propertyView, String... propertySet) {

		Set<String> properties = getPropertySet(type, propertyView);
		for(String property : propertySet) {
			properties.add(property);
		}
	}

	public static Set<String> getPropertySet(Class type, PropertyView propertyView) {

		Map<PropertyView, Set<String>> propertyViewMap = getPropertyViewForType(type);
		Set<String> propertySet = propertyViewMap.get(propertyView);

		if(propertySet == null) {
			propertySet = new LinkedHashSet<String>();
			propertyViewMap.put(propertyView, propertySet);
		}

		return propertySet;
	}

	// ----- private methods -----
	private static String convertName(Class type) {

		return(type.getSimpleName().toLowerCase());
	}

	private static Map<String, DirectedRelationship> getTypeMap(String sourceType) {

		Map<String, DirectedRelationship> typeMap = globalTypeMap.get(sourceType);
		if(typeMap == null) {
			typeMap = new LinkedHashMap<String, DirectedRelationship>();
			globalTypeMap.put(sourceType, typeMap);
		}

		return(typeMap);
	}

	private static Map<PropertyView, Set<String>> getPropertyViewForType(Class type) {

		Map<PropertyView, Set<String>> propertyViewMap = globalPropertyKeyMap.get(type);
		if(propertyViewMap == null) {
			propertyViewMap = new LinkedHashMap<PropertyView, Set<String>>();
			globalPropertyKeyMap.put(type, propertyViewMap);
		}

		return propertyViewMap;
	}
}
