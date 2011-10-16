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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.core.PropertyValidator;
import org.structr.core.Value;
import org.structr.core.entity.DirectedRelationship;

/**
 * A global context for functional mappings between nodes and
 * relationships, property views and property validators.
 *
 * @author Christian Morgner
 */
public class EntityContext {

	private static final Map<Class, Map<String, Class<? extends PropertyValidator>>> globalValidatorMap = new LinkedHashMap<Class, Map<String, Class<? extends PropertyValidator>>>();
	private static final Map<String, Map<String, DirectedRelationship>> globalTypeMap = new LinkedHashMap<String, Map<String, DirectedRelationship>>();
	private static final Map<Class, Map<PropertyView, Set<String>>> globalPropertyKeyMap = new LinkedHashMap<Class, Map<PropertyView, Set<String>>>();
	private static final Map<Class, Map<String, Value>> globalValidatonParameterMap = new LinkedHashMap<Class, Map<String, Value>>();

	private static final Logger logger = Logger.getLogger(EntityContext.class.getName());

	// ----- static relationship methods -----
	public static DirectedRelationship getRelation(String sourceType, String destType) {

		// FIXME: using Strings here breaks the type saftey, should use Class instead
		//        but incompatible with REST TypeConstraint!
		return getRelationshipMapForType(sourceType).get(destType);
	}

	public static DirectedRelationship getRelation(Class sourceType, Class destType) {
		return getRelationshipMapForType(convertName(sourceType)).get(convertName(destType));
	}

	public static void registerRelation(Class sourceType, Class destType, RelationshipType relType, Direction direction) {
		registerRelation(convertName(sourceType), convertName(destType), relType, direction);
	}

	// ----- property set methods -----
	public static void registerPropertySet(Class type, PropertyView propertyView, String... propertySet) {

		Set<String> properties = getPropertySet(type, propertyView);
		for(String property : propertySet) {

			if("id".equals(property.toLowerCase()) || "type".equals(property.toLowerCase())) {
				logger.log(Level.SEVERE, "id and type are not allowed in property views, ignoring!");
			} else {
				properties.add(property);
			}
		}
	}

	public static Set<String> getPropertySet(Class type, PropertyView propertyView) {

		Map<PropertyView, Set<String>> propertyViewMap = getPropertyViewMapForType(type);
		Set<String> propertySet = propertyViewMap.get(propertyView);

		if(propertySet == null) {
			propertySet = new LinkedHashSet<String>();
			propertyViewMap.put(propertyView, propertySet);
		}

		return propertySet;
	}

	// ----- validator methods -----
	public static void registerPropertyValidator(Class type, String propertyKey, Class<? extends PropertyValidator> validatorClass) {
		registerPropertyValidator(type, propertyKey, validatorClass, null);
	}

	public static void registerPropertyValidator(Class type, String propertyKey, Class<? extends PropertyValidator> validatorClass, Value parameter) {

		Map<String, Class<? extends PropertyValidator>> validatorMap = getPropertyValidatorMapForType(type);
		validatorMap.put(propertyKey, validatorClass);

		if(parameter != null) {
			Map<String, Value> validatorParameterMap = getPropertyValidatonParameterMapForType(type);
			validatorParameterMap.put(propertyKey, parameter);
		}
	}

	public static PropertyValidator getPropertyValidator(Class type, String propertyKey) {

		Map<String, Class<? extends PropertyValidator>> validatorMap = getPropertyValidatorMapForType(type);
		Class clazz = validatorMap.get(propertyKey);
		PropertyValidator validator = null;

		if(clazz != null) {

			try {
				validator = (PropertyValidator)clazz.newInstance();

			} catch(Throwable t) {
				logger.log(Level.WARNING, "Unable to instantiate validator {0}: {1}", new Object[] { clazz.getName(), t.getMessage() } );
			}
		}

		return validator;
	}

	public static Value getPropertyValidationParameter(Class type, String propertyKey) {

		Map<String, Value> validationParameterMap = getPropertyValidatonParameterMapForType(type);
		if(validationParameterMap != null) {
			return validationParameterMap.get(propertyKey);
		}

		return null;
	}

	// ----- private methods -----
	private static String convertName(Class type) {

		return(type.getSimpleName().toLowerCase());
	}

	private static void registerRelation(String sourceType, String destType, RelationshipType relType, Direction direction) {

		Map<String, DirectedRelationship> typeMap = getRelationshipMapForType(sourceType);
		typeMap.put(destType, new DirectedRelationship(relType, direction));
	}

	private static Map<String, DirectedRelationship> getRelationshipMapForType(String sourceType) {

		Map<String, DirectedRelationship> typeMap = globalTypeMap.get(sourceType);
		if(typeMap == null) {
			typeMap = new LinkedHashMap<String, DirectedRelationship>();
			globalTypeMap.put(sourceType, typeMap);
		}

		return(typeMap);
	}

	private static Map<PropertyView, Set<String>> getPropertyViewMapForType(Class type) {

		Map<PropertyView, Set<String>> propertyViewMap = globalPropertyKeyMap.get(type);
		if(propertyViewMap == null) {
			propertyViewMap = new LinkedHashMap<PropertyView, Set<String>>();
			globalPropertyKeyMap.put(type, propertyViewMap);
		}

		return propertyViewMap;
	}

	private static Map<String, Class<? extends PropertyValidator>> getPropertyValidatorMapForType(Class type) {

		Map<String, Class<? extends PropertyValidator>> validatorMap = globalValidatorMap.get(type);
		if(validatorMap == null) {
			validatorMap = new LinkedHashMap<String, Class<? extends PropertyValidator>>();
			globalValidatorMap.put(type, validatorMap);
		}

		return validatorMap;
	}

	private static Map<String, Value> getPropertyValidatonParameterMapForType(Class type) {

		Map<String, Value> validationParameterMap = globalValidatonParameterMap.get(type);
		if(validationParameterMap == null) {
			validationParameterMap = new LinkedHashMap<String, Value>();
			globalValidatonParameterMap.put(type, validationParameterMap);
		}

		return validationParameterMap;
	}
}
