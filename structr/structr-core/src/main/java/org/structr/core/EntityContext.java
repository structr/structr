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

package org.structr.core;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.entity.DirectedRelationship;

/**
 * A global context for functional mappings between nodes and
 * relationships, property views and property validators.
 *
 * @author Christian Morgner
 */
public class EntityContext {

	private static final Map<Class, Map<String, Class<? extends PropertyConverter>>> globalPropertyConverterMap = new LinkedHashMap<Class, Map<String, Class<? extends PropertyConverter>>>();
	private static final Map<Class, Map<String, Class<? extends PropertyValidator>>> globalValidatorMap = new LinkedHashMap<Class, Map<String, Class<? extends PropertyValidator>>>();
	private static final Map<String, Map<String, DirectedRelationship>> globalTypeMap = new LinkedHashMap<String, Map<String, DirectedRelationship>>();
	private static final Map<Class, Map<PropertyView, Set<String>>> globalPropertyKeyMap = new LinkedHashMap<Class, Map<PropertyView, Set<String>>>();
	private static final Map<Class, Map<String, Value>> globalValidationParameterMap = new LinkedHashMap<Class, Map<String, Value>>();
	private static final Map<Class, Map<String, Value>> globalConversionParameterMap = new LinkedHashMap<Class, Map<String, Value>>();

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
	public static void registerPropertySet(Class type, PropertyView propertyView, PropertyKey... propertySet) {

		Set<String> properties = getPropertySet(type, propertyView);
		for(PropertyKey property : propertySet) {
			properties.add(property.name());
		}

		// include property sets from superclass
		Class superClass = type.getSuperclass();
		while(superClass != null && !superClass.equals(Object.class)) {
			
			Set<String> superProperties = getPropertySet(superClass, propertyView);
			properties.addAll(superProperties);

			// one level up :)
			superClass = superClass.getSuperclass();
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

		Map<String, Class<? extends PropertyValidator>> validatorMap = null;
		PropertyValidator validator = null;
		Class localType = type;
		Class clazz = null;
		
		// try all superclasses
		while(clazz == null && !localType.equals(Object.class)) {
			validatorMap = getPropertyValidatorMapForType(localType);
			clazz = validatorMap.get(propertyKey);
			
//			logger.log(Level.INFO, "Validator class {0} found for type {1}", new Object[] { clazz != null ? clazz.getSimpleName() : "null", localType } );

			// one level up :)
			localType = localType.getSuperclass();
		}

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

		Map<String, Value> validationParameterMap = null;
		Class localType = type;
		Value value = null;
		
		while(value == null && !localType.equals(Object.class)) {
			validationParameterMap = getPropertyValidatonParameterMapForType(localType);
			value = validationParameterMap.get(propertyKey);
			
//			logger.log(Level.INFO, "Validation parameter value {0} found for type {1}", new Object[] { value != null ? value.getClass().getSimpleName() : "null", localType } );
			
			localType = localType.getSuperclass();
		}

		return value;
	}

	// ----- PropertyConverter methods -----
	public static void registerPropertyConverter(Class type, String propertyKey, Class<? extends PropertyConverter> propertyConverterClass) {
		registerPropertyConverter(type, propertyKey, propertyConverterClass, null);
	}
	
	public static void registerPropertyConverter(Class type, String propertyKey, Class<? extends PropertyConverter> propertyConverterClass, Value value) {
		getPropertyConverterMapForType(type).put(propertyKey, propertyConverterClass);
		
		if(value != null) {
			getPropertyConversionParameterMapForType(type).put(propertyKey, value);
		}
	}

	public static PropertyConverter getPropertyConverter(Class type, String propertyKey) {

		Map<String, Class<? extends PropertyConverter>> converterMap = null;
		PropertyConverter propertyConverter = null;
		Class localType = type;
		Class clazz = null;
		
		while(clazz == null && !localType.equals(Object.class)) {
			converterMap = getPropertyConverterMapForType(localType);
			clazz = converterMap.get(propertyKey);
			
//			logger.log(Level.INFO, "Converter class {0} found for type {1}", new Object[] { clazz != null ? clazz.getSimpleName() : "null", localType } );
			
			localType = localType.getSuperclass();
		}
		
		if(clazz != null) {

			try {
				propertyConverter = (PropertyConverter)clazz.newInstance();

			} catch(Throwable t) {
				logger.log(Level.WARNING, "Unable to instantiate property PropertyConverter {0}: {1}", new Object[] { clazz.getName(), t.getMessage() } );
			}
		}

		return propertyConverter;
	}

	public static Value getPropertyConversionParameter(Class type, String propertyKey) {

		Map<String, Value> conversionParameterMap = null;
		Class localType = type;
		Value value = null;
		
		while(value == null && !localType.equals(Object.class)) {
			conversionParameterMap = getPropertyConversionParameterMapForType(localType);
			value = conversionParameterMap.get(propertyKey);
			
//			logger.log(Level.INFO, "Conversion parameter value {0} found for type {1}", new Object[] { value != null ? value.getClass().getSimpleName() : "null", localType } );
			
			localType = localType.getSuperclass();
		}

		return value;
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
			propertyViewMap = new EnumMap<PropertyView, Set<String>>(PropertyView.class);
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

		Map<String, Value> validationParameterMap = globalValidationParameterMap.get(type);
		if(validationParameterMap == null) {
			validationParameterMap = new LinkedHashMap<String, Value>();
			globalValidationParameterMap.put(type, validationParameterMap);
		}

		return validationParameterMap;
	}

	private static Map<String, Class<? extends PropertyConverter>> getPropertyConverterMapForType(Class type) {

		Map<String, Class<? extends PropertyConverter>> PropertyConverterMap = globalPropertyConverterMap.get(type);
		if(PropertyConverterMap == null) {
			PropertyConverterMap = new LinkedHashMap<String, Class<? extends PropertyConverter>>();
			globalPropertyConverterMap.put(type, PropertyConverterMap);
		}

		return PropertyConverterMap;
	}

	private static Map<String, Value> getPropertyConversionParameterMapForType(Class type) {

		Map<String, Value> conversionParameterMap = globalConversionParameterMap.get(type);
		if(conversionParameterMap == null) {
			conversionParameterMap = new LinkedHashMap<String, Value>();
			globalConversionParameterMap.put(type, conversionParameterMap);
		}

		return conversionParameterMap;
	}
}
