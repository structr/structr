/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.apache.commons.lang.StringUtils;


import org.structr.common.CaseHelper;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.*;
import org.structr.core.property.GenericProperty;
import org.structr.core.module.ModuleService;

//~--- classes ----------------------------------------------------------------

/**
 * A global context for functional mappings between nodes and relationships,
 * property views and property validators.
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class EntityContext {

	private static final Logger logger                                                            = Logger.getLogger(EntityContext.class.getName());

	private static final Map<Class, Map<PropertyKey, Set<PropertyValidator>>> globalValidatorMap  = new LinkedHashMap<>();
	private static final Map<Class, Map<String, Set<PropertyKey>>> globalPropertyViewMap          = new LinkedHashMap<>();
	private static final Map<Class, Map<String, PropertyKey>> globalClassDBNamePropertyMap        = new LinkedHashMap<>();
	private static final Map<Class, Map<String, PropertyKey>> globalClassJSNamePropertyMap        = new LinkedHashMap<>();
	private static final Map<Class, Map<String, PropertyGroup>> globalAggregatedPropertyGroupMap  = new LinkedHashMap<>();
	private static final Map<Class, Map<String, PropertyGroup>> globalPropertyGroupMap            = new LinkedHashMap<>();
	private static final Map<Class, Map<String, ViewTransformation>> viewTransformations          = new LinkedHashMap<>();
	private static final Map<Class, Set<Transformation<GraphObject>>> globalTransformationMap     = new LinkedHashMap<>();
	private static final Map<String, String> normalizedEntityNameCache                            = new LinkedHashMap<>();
	private static final Map<Class, Set<Method>> exportedMethodMap                                = new LinkedHashMap<>();
	private static final Map<Class, Set<Class>> interfaceMap                                      = new LinkedHashMap<>();
	private static final Map<String, Class> reverseInterfaceMap                                   = new LinkedHashMap<>();
	private static final Set<PropertyKey> globalKnownPropertyKeys                                 = new LinkedHashSet<>();

	private static FactoryDefinition factoryDefinition                                            = new DefaultFactoryDefinition();
	private static ModuleService staticModuleService                                              = null;

	//~--- methods --------------------------------------------------------

	/**
	 * Initialize the entity context for the given class.
	 * This method sets defaults common for any class, f.e. registers any of its parent properties.
	 *
	 * @param type
	 */
	public static void init(Class type) {

		// moved here from scanEntity, no reason to have this in a separate
		// method requiring two different calls instead of one
		int modifiers = type.getModifiers();
		if (!Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)) {
			
			try {
				
				Object entity                         = type.newInstance();
				Map<Field, PropertyKey> allProperties = getFieldValuesOfType(PropertyKey.class, entity);
				Map<Field, View> views                = getFieldValuesOfType(View.class, entity);
				Class entityType                      = entity.getClass();

				for (Entry<Field, PropertyKey> entry : allProperties.entrySet()) {

					PropertyKey propertyKey = entry.getValue();
					Field field             = entry.getKey();
					Class declaringClass    = field.getDeclaringClass();

					if (declaringClass != null) {

						propertyKey.setDeclaringClass(declaringClass);
						registerProperty(declaringClass, propertyKey);

					}

					registerProperty(entityType, propertyKey);
				}

				for (Entry<Field, View> entry : views.entrySet()) {

					Field field = entry.getKey();
					View view   = entry.getValue();

					for (PropertyKey propertyKey : view.properties()) {

						// register field in view for entity class and declaring superclass
						registerPropertySet(field.getDeclaringClass(), view.name(), propertyKey);
						registerPropertySet(entityType, view.name(), propertyKey);
					}
				}
				
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Unable to instantiate {0}: {1}", new Object[] { type, t.getMessage() } );
			}
		}
		
		Set<Method> typeMethods = exportedMethodMap.get(type);
		if (typeMethods == null) {
			typeMethods = new LinkedHashSet<>();
			exportedMethodMap.put(type, typeMethods);
		}
		
		typeMethods.addAll(getAnnotatedMethods(type, Export.class));
	}
	
	public static void registerProperty(Class type, PropertyKey propertyKey) {
		
		getClassDBNamePropertyMapForType(type).put(propertyKey.dbName(),   propertyKey);
		getClassJSNamePropertyMapForType(type).put(propertyKey.jsonName(), propertyKey);
		
		registerPropertySet(type, PropertyView.All, propertyKey);
		
		// inform property key of its registration
		propertyKey.registrationCallback(type);
	}
	
	/**
	 * Register a transformation that will be applied to every newly created entity of a given type.
	 * 
	 * @param type the type of the entities for which the transformation should be applied
	 * @param transformation the transformation to apply on every entity
	 */
	public static void registerEntityCreationTransformation(Class type, Transformation<GraphObject> transformation) {
		getEntityCreationTransformationsForType(type).add(transformation);
	}

	/**
	 * Registers a property group for the given key of the given entity type. A property group can be
	 * used to combine a set of properties into an object. {@see PropertyGroup}
	 * 
	 * @param type the type of the entities for which the property group should be registered
	 * @param key the property key under which the property group should be visible
	 * @param propertyGroup the property group
	 */
	public static void registerPropertyGroup(Class type, PropertyKey key, PropertyGroup propertyGroup) {
		getPropertyGroupMapForType(type).put(key.dbName(), propertyGroup);
	}

	// ----- property set methods -----
	/**
	 * Registers the given set of property keys for the view with name <code>propertyView</code>
	 * and the given prefix of entities with the given type.
	 * 
	 * @param type the type of the entities for which the view will be registered
	 * @param propertyView the name of the property view for which the property set will be registered
	 * @param viewPrefix a string that will be prepended to all property keys in this view
	 * @param propertySet the set of property keys to register for the given view
	 */
	public static void registerPropertySet(Class type, String propertyView, PropertyKey... propertySet) {

		Map<String, Set<PropertyKey>> propertyViewMap = getPropertyViewMapForType(type);
		Set<PropertyKey> properties                   = propertyViewMap.get(propertyView);
		
		if (properties == null) {
			properties = new LinkedHashSet<>();
			propertyViewMap.put(propertyView, properties);
		}

		// add all properties from set
		properties.addAll(Arrays.asList(propertySet));
	}

	// ----- private methods -----
	/**
	 * Tries to normalize (and singularize) the given string so that it resolves to
	 * an existing entity type.
	 * 
	 * @param possibleEntityString
	 * @return the normalized entity name in its singular form
	 */
	public static String normalizeEntityName(String possibleEntityString) {

		if (possibleEntityString == null) {

			return null;

		}
		
		if ("/".equals(possibleEntityString)) {
			
			return "/";
			
		}

                StringBuilder result = new StringBuilder();
                
		if (possibleEntityString.contains("/")) {

			String[] names           = StringUtils.split(possibleEntityString, "/");

			for (String possibleEntityName : names) {

				// CAUTION: this cache might grow to a very large size, as it
				// contains all normalized mappings for every possible
				// property key / entity name that is ever called.
				String normalizedType = normalizedEntityNameCache.get(possibleEntityName);

				if (normalizedType == null) {

					normalizedType = StringUtils.capitalize(CaseHelper.toUpperCamelCase(possibleEntityName));
					
					if (normalizedType.endsWith("ies")) {

						normalizedType = normalizedType.substring(0, normalizedType.length() - 3).concat("y");

					} else if (!normalizedType.endsWith("ss") && normalizedType.endsWith("s")) {

						logger.log(Level.FINEST, "Removing trailing plural 's' from type {0}", normalizedType);

						normalizedType = normalizedType.substring(0, normalizedType.length() - 1);
					}
				}

				result.append(normalizedType).append("/");

			}

			return StringUtils.removeEnd(result.toString(), "/");

		} else {

//                      CAUTION: this cache might grow to a very large size, as it
			// contains all normalized mappings for every possible
			// property key / entity name that is ever called.
			String normalizedType = normalizedEntityNameCache.get(possibleEntityString);

			if (normalizedType == null) {

				normalizedType = StringUtils.capitalize(CaseHelper.toUpperCamelCase(possibleEntityString));

				if (normalizedType.endsWith("ies")) {

					normalizedType = normalizedType.substring(0, normalizedType.length() - 3).concat("y");

				} else if (!normalizedType.endsWith("ss") && normalizedType.endsWith("s")) {

					logger.log(Level.FINEST, "Removing trailing plural 's' from type {0}", normalizedType);

					normalizedType = normalizedType.substring(0, normalizedType.length() - 1);
				}
			}

			return normalizedType;
		}
	}
	
	/**
	 * Converts a Java class name (entity name) into a valid REST resource name.
	 * 
	 * @param normalizedEntityName
	 * @return the given string in lowercase, with camel case occurences replaced by underscores
	 */
	public static String denormalizeEntityName(String normalizedEntityName) {
		
		StringBuilder buf = new StringBuilder();
		
		for (char c : normalizedEntityName.toCharArray()) {
			
			if (Character.isUpperCase(c) && buf.length() > 0) {
				buf.append("_");
			}
			
			buf.append(Character.toLowerCase(c));
		}
		
		return buf.toString();
	}
	
	public static void registerConvertedProperty(PropertyKey propertyKey) {
		globalKnownPropertyKeys.add(propertyKey);
	}

	//~--- get methods ----------------------------------------------------

	public static Class getEntityClassForRawType(final String rawType) {
		
		// first try: raw name
		Class type = getEntityClassForRawType(rawType, false);
		if (type == null) {
			
			// second try: normalized name
			type = getEntityClassForRawType(rawType, true);
		}
		
		return type;
	}
	
	private static Class getEntityClassForRawType(final String rawType, final boolean normalize) {

		final String normalizedEntityName = normalize ? normalizeEntityName(rawType) : rawType;
		final ModuleService moduleService = getModuleService();

		// first try: node entity
		Class type = moduleService.getNodeEntityClass(normalizedEntityName);
		
		// second try: relationship entity
		if (type == null) {
			type = moduleService.getRelationshipEntityClass(normalizedEntityName);
		}
		
		// third try: interface
		if (type == null) {
			type = reverseInterfaceMap.get(normalizedEntityName);

		}

		// store type but only if it exists!
		if (type != null) {
			normalizedEntityNameCache.put(rawType, type.getSimpleName());
		}

		return type;
	}

	public static Set<Transformation<GraphObject>> getEntityCreationTransformations(Class type) {

		Set<Transformation<GraphObject>> transformations = new TreeSet<>();
		Class localType                                  = type;

		// collect for all superclasses
		while (localType != null && !localType.equals(Object.class)) {

			transformations.addAll(getEntityCreationTransformationsForType(localType));

			localType = localType.getSuperclass();

		}

		return transformations;
	}

	// ----- property notions -----
	public static PropertyGroup getPropertyGroup(Class type, PropertyKey key) {
		return getPropertyGroup(type, key.dbName());
	}

	public static PropertyGroup getPropertyGroup(Class type, String key) {

		PropertyGroup group = getAggregatedPropertyGroupMapForType(type).get(key);
		if(group == null) {
			
			Class localType     = type;

			while(group == null && localType != null && !localType.equals(Object.class)) {

				group = getPropertyGroupMapForType(localType).get(key);

				if(group == null) {

					// try interfaces as well
					for(Class interfaceClass : getInterfacesForType(localType)) {

						group = getPropertyGroupMapForType(interfaceClass).get(key);
						if(group != null) {
							break;
						}
					}
				}

				localType = localType.getSuperclass();
			}
			
			getAggregatedPropertyGroupMapForType(type).put(key, group);
		}
		
		return group;
	}

	// ----- view transformations -----
	public static void registerViewTransformation(Class type, String view, ViewTransformation transformation) {
		getViewTransformationMapForType(type).put(view, transformation);
	}
	
	public static ViewTransformation getViewTransformation(Class type, String view) {
		return getViewTransformationMapForType(type).get(view);
	}
	
	private static Map<String, ViewTransformation> getViewTransformationMapForType(Class type) {
		
		Map<String, ViewTransformation> viewTransformationMap = viewTransformations.get(type);
		if(viewTransformationMap == null) {
			viewTransformationMap = new LinkedHashMap<>();
			viewTransformations.put(type, viewTransformationMap);
		}
		
		return viewTransformationMap;
	}
	

	// ----- property set methods -----
	public static Set<String> getPropertyViews() {

		Set<String> views = new LinkedHashSet<>();
		
		// add all existing views
		for (Map<String, Set<PropertyKey>> view : globalPropertyViewMap.values()) {
			views.addAll(view.keySet());
		}
		
		return Collections.unmodifiableSet(views);
	}
	
	public static Set<PropertyKey> getPropertySet(Class type, String propertyView) {

		Map<String, Set<PropertyKey>> propertyViewMap = getPropertyViewMapForType(type);
		Set<PropertyKey> properties                   = propertyViewMap.get(propertyView);

		if (properties == null) {
			properties = new LinkedHashSet<>();
		}
		
		// read-only
		return Collections.unmodifiableSet(properties);
	}
	
	public static PropertyKey getPropertyKeyForDatabaseName(Class type, String dbName) {
		return getPropertyKeyForDatabaseName(type, dbName, true);
	}
	
	public static PropertyKey getPropertyKeyForDatabaseName(Class type, String dbName, boolean createGeneric) {

		Map<String, PropertyKey> classDBNamePropertyMap = getClassDBNamePropertyMapForType(type);
		PropertyKey key                                 = classDBNamePropertyMap.get(dbName);
		
		if (key == null) {
			
			// first try: uuid
			if (GraphObject.uuid.dbName().equals(dbName)) {
				return GraphObject.uuid;
			}

			if (createGeneric) {
				key = new GenericProperty(dbName);
			}
		}
		
		return key;
	}
	
	public static PropertyKey getPropertyKeyForJSONName(Class type, String jsonName) {
		return getPropertyKeyForJSONName(type, jsonName, true);
	}
	
	public static PropertyKey getPropertyKeyForJSONName(Class type, String jsonName, boolean createIfNotFound) {

		if (jsonName == null) {
			return null;
		}

		Map<String, PropertyKey> classJSNamePropertyMap = getClassJSNamePropertyMapForType(type);
		PropertyKey key                                 = classJSNamePropertyMap.get(jsonName);
		
		if (key == null) {
			
			// first try: uuid
			if (GraphObject.uuid.dbName().equals(jsonName)) {
				
				return GraphObject.uuid;
			}

			if (createIfNotFound) {
				
				key = new GenericProperty(jsonName);
			}
		}
		
		return key;
	}

	public static Set<PropertyValidator> getPropertyValidators(final SecurityContext securityContext, Class type, PropertyKey propertyKey) {

		Set<PropertyValidator> validators                     = new LinkedHashSet<>();
		Map<PropertyKey, Set<PropertyValidator>> validatorMap = null;
		Class localType                                       = type;

		// try all superclasses
		while (localType != null && !localType.equals(Object.class)) {

			validatorMap = getPropertyValidatorMapForType(localType);
			
			Set<PropertyValidator> classValidators = validatorMap.get(propertyKey);
			if(classValidators != null) {
				validators.addAll(validatorMap.get(propertyKey));
			}

			// try converters from interfaces as well
			for(Class interfaceClass : getInterfacesForType(localType)) {
				Set<PropertyValidator> interfaceValidators = getPropertyValidatorMapForType(interfaceClass).get(propertyKey);
				if(interfaceValidators != null) {
					validators.addAll(interfaceValidators);
				}
			}
			
//                      logger.log(Level.INFO, "Validator class {0} found for type {1}", new Object[] { clazz != null ? clazz.getSimpleName() : "null", localType } );
			// one level up :)
			localType = localType.getSuperclass();

		}

		return validators;
	}

	private static Map<String, Set<PropertyKey>> getPropertyViewMapForType(Class type) {

		Map<String, Set<PropertyKey>> propertyViewMap = globalPropertyViewMap.get(type);

		if (propertyViewMap == null) {

			propertyViewMap = new LinkedHashMap<>();

			globalPropertyViewMap.put(type, propertyViewMap);

		}

		return propertyViewMap;
	}

	private static Map<String, PropertyKey> getClassDBNamePropertyMapForType(Class type) {

		Map<String, PropertyKey> classDBNamePropertyMap = globalClassDBNamePropertyMap.get(type);

		if (classDBNamePropertyMap == null) {

			classDBNamePropertyMap = new LinkedHashMap<>();

			globalClassDBNamePropertyMap.put(type, classDBNamePropertyMap);

		}

		return classDBNamePropertyMap;
	}

	private static Map<String, PropertyKey> getClassJSNamePropertyMapForType(Class type) {

		Map<String, PropertyKey> classJSNamePropertyMap = globalClassJSNamePropertyMap.get(type);

		if (classJSNamePropertyMap == null) {

			classJSNamePropertyMap = new LinkedHashMap<>();

			globalClassJSNamePropertyMap.put(type, classJSNamePropertyMap);

		}

		return classJSNamePropertyMap;
	}

	private static Map<PropertyKey, Set<PropertyValidator>> getPropertyValidatorMapForType(Class type) {

		Map<PropertyKey, Set<PropertyValidator>> validatorMap = globalValidatorMap.get(type);

		if (validatorMap == null) {

			validatorMap = new LinkedHashMap<>();

			globalValidatorMap.put(type, validatorMap);

		}

		return validatorMap;
	}

	private static Map<String, PropertyGroup> getAggregatedPropertyGroupMapForType(Class type) {

		Map<String, PropertyGroup> groupMap = globalAggregatedPropertyGroupMap.get(type);

		if (groupMap == null) {

			groupMap = new LinkedHashMap<>();

			globalAggregatedPropertyGroupMap.put(type, groupMap);

		}

		return groupMap;
	}

	private static Map<String, PropertyGroup> getPropertyGroupMapForType(Class type) {

		Map<String, PropertyGroup> groupMap = globalPropertyGroupMap.get(type);

		if (groupMap == null) {

			groupMap = new LinkedHashMap<>();

			globalPropertyGroupMap.put(type, groupMap);

		}

		return groupMap;
	}

	private static Set<Transformation<GraphObject>> getEntityCreationTransformationsForType(Class type) {

		Set<Transformation<GraphObject>> transformations = globalTransformationMap.get(type);

		if (transformations == null) {

			transformations = new LinkedHashSet<>();

			globalTransformationMap.put(type, transformations);

		}

		return transformations;
	}
	
	public static Set<Class> getInterfacesForType(Class type) {
		
		Set<Class> interfaces = interfaceMap.get(type);
		if(interfaces == null) {
			
			interfaces = new LinkedHashSet<>();
			interfaceMap.put(type, interfaces);
			
			for(Class iface : type.getInterfaces()) {

				reverseInterfaceMap.put(iface.getSimpleName(), iface);
				interfaces.add(iface);
			}
		}
		
		return interfaces;
	}
	
	public static Set<Method> getExportedMethodsForType(Class type) {
		return exportedMethodMap.get(type);
	}
	
	public static boolean isKnownProperty(final PropertyKey key) {
		return globalKnownPropertyKeys.contains(key);
	}

	public static FactoryDefinition getFactoryDefinition() {
		return factoryDefinition;
	}
	
	public static void registerFactoryDefinition(FactoryDefinition factory) {
		factoryDefinition = factory;
	}
	
	public static Set<Method> getAnnotatedMethods(Class entityType, Class annotationType) {
		
		Set<Method> methods    = new LinkedHashSet<>();
		Set<Class<?>> allTypes = getAllTypes(entityType);
		
		for (Class<?> type : allTypes) {
			
			for (Method method : type.getDeclaredMethods()) {
				
				if (method.getAnnotation(annotationType) != null) {
				
					methods.add(method);
				}
			}
		}
		
		return methods;
	}
	
	private static <T> Map<Field, T> getFieldValuesOfType(Class<T> entityType, Object entity) {
		
		Map<Field, T> fields   = new LinkedHashMap<>();
		Set<Class<?>> allTypes = getAllTypes(entity.getClass());
		
		for (Class<?> type : allTypes) {
			
			for (Field field : type.getDeclaredFields()) {

				if (entityType.isAssignableFrom(field.getType())) {

					try {
						fields.put(field, (T)field.get(entity));

					} catch(Throwable t) { }
				}
			}
		}
		
		return fields;
	}
	
	private static Set<Class<?>> getAllTypes(Class<?> type) {

		Set<Class<?>> types = new LinkedHashSet<>();
		Class localType     = type;
			
		do {
			
			collectAllInterfaces(localType, types);
			types.add(localType);
			
			localType = localType.getSuperclass();

		} while (!localType.equals(Object.class));
		
		return types;
	}
	
	private static void collectAllInterfaces(Class<?> type, Set<Class<?>> interfaces) {

		if (interfaces.contains(type)) {
			return;
		}
		
		for (Class iface : type.getInterfaces()) {
			
			collectAllInterfaces(iface, interfaces);
			interfaces.add(iface);
		}
	}
	
	private static ModuleService getModuleService() {
		
		if (staticModuleService == null) {
			staticModuleService = Services.getService(ModuleService.class);
		}
		
		return staticModuleService;
	}
}
