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

import org.structr.core.graph.NodeService;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.RelationshipType;

import org.structr.common.CaseHelper;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.entity.RelationshipMapping;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.DynamicRelationshipType;
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

	private static final String COMBINED_RELATIONSHIP_KEY_SEP                                     = " ";
	private static final Logger logger                                                            = Logger.getLogger(EntityContext.class.getName());

	private static final Map<Class, Map<PropertyKey, Set<PropertyValidator>>> globalValidatorMap  = new LinkedHashMap<Class, Map<PropertyKey, Set<PropertyValidator>>>();
	private static final Map<Class, Map<String, Set<PropertyKey>>> globalSearchablePropertyMap    = new LinkedHashMap<Class, Map<String, Set<PropertyKey>>>();

	// This map contains a mapping from (sourceType, destType) -> Relation
	private static final Map<Class, Map<String, Set<PropertyKey>>> globalPropertyViewMap          = new LinkedHashMap<Class, Map<String, Set<PropertyKey>>>();
	private static final Map<Class, Map<String, PropertyKey>> globalClassDBNamePropertyMap        = new LinkedHashMap<Class, Map<String, PropertyKey>>();
	private static final Map<Class, Map<String, PropertyKey>> globalClassJSNamePropertyMap        = new LinkedHashMap<Class, Map<String, PropertyKey>>();

	// This map contains a mapping from (sourceType, propertyKey) -> Relation
	private static final Map<Class, Map<String, PropertyGroup>> globalAggregatedPropertyGroupMap  = new LinkedHashMap<Class, Map<String, PropertyGroup>>();
	private static final Map<Class, Map<String, PropertyGroup>> globalPropertyGroupMap            = new LinkedHashMap<Class, Map<String, PropertyGroup>>();

	// This map contains view-dependent result set transformations
	private static final Map<Class, Map<String, ViewTransformation>> viewTransformations          = new LinkedHashMap<Class, Map<String, ViewTransformation>>();
	
	// This set contains all known properties
	private static final Set<PropertyKey> globalKnownPropertyKeys                                 = new LinkedHashSet<PropertyKey>();
	private static final Map<Class, Set<Transformation<GraphObject>>> globalTransformationMap     = new LinkedHashMap<Class, Set<Transformation<GraphObject>>>();
	private static final Map<String, String> normalizedEntityNameCache                            = new LinkedHashMap<String, String>();
	private static final Map<String, RelationshipMapping> globalRelationshipNameMap               = new LinkedHashMap<String, RelationshipMapping>();
	private static final Map<String, Class> globalRelationshipClassMap                            = new LinkedHashMap<String, Class>();
	private static final Map<Class, Set<Class>> interfaceMap                                      = new LinkedHashMap<Class, Set<Class>>();
	private static final Map<String, Class> reverseInterfaceMap                                   = new LinkedHashMap<String, Class>();
	private static Map<String, Class> cachedEntities                                              = new LinkedHashMap<String, Class>();

	private static FactoryDefinition factoryDefinition                                            = new DefaultFactoryDefinition();

	//~--- methods --------------------------------------------------------

	/**
	 * Initialize the entity context for the given class.
	 * This method sets defaults common for any class, f.e. registers any of its parent properties.
	 *
	 * @param type
	 */
	public static void init(Class type) {

		// 1. Register searchable keys of superclasses
		for (Enum index : NodeService.NodeIndex.values()) {

			String indexName                                           = index.name();
			Map<String, Set<PropertyKey>> searchablePropertyMapForType = getSearchablePropertyMapForType(type);
			Set<PropertyKey> searchablePropertySet                     = searchablePropertyMapForType.get(indexName);

			if (searchablePropertySet == null) {

				searchablePropertySet = new LinkedHashSet<PropertyKey>();

				searchablePropertyMapForType.put(indexName, searchablePropertySet);

			}

			Class localType = type.getSuperclass();

			while ((localType != null) &&!localType.equals(Object.class)) {

				Set<PropertyKey> superProperties = getSearchableProperties(localType, indexName);
				searchablePropertySet.addAll(superProperties);

				// include property sets from interfaces
				for(Class interfaceClass : getInterfacesForType(localType)) {
					searchablePropertySet.addAll(getSearchableProperties(interfaceClass, indexName));
				}

				// one level up :)
				localType = localType.getSuperclass();

			}
		}
		
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
	}
	
	public static void registerProperty(Class type, PropertyKey propertyKey) {
		
		getClassDBNamePropertyMapForType(type).put(propertyKey.dbName(),   propertyKey);
		getClassJSNamePropertyMapForType(type).put(propertyKey.jsonName(), propertyKey);
		
		registerPropertySet(type, PropertyView.All, propertyKey);
		
		// inform property key of its registration
		propertyKey.registrationCallback(type);
	}
	
	/**
	 * Initialize the entity context with all classes from the module service,
	 */
	public static void init() {

		cachedEntities = Services.getService(ModuleService.class).getCachedNodeEntities();
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

	// ----- named relations -----
	/**
	 * Registers a relationship entity with the given name and type to be instantiated for relationships
	 * with type <code>relType</code> from <code>sourceType</code> to <code>destType</code>. Relationship
	 * entity types registered with this method will be instantiated when a database relationship with
	 * the given parameters are encountered.
	 * 
	 * @param relationName the name of the relationship as it should appear in the REST resource
	 * @param relationshipEntityType the type of the relationship entity
	 * @param sourceType the type of the source entity
	 * @param destType the type of the destination entity
	 * @param relType the relationship type
	 */
	public static void registerNamedRelation(String relationName, Class relationshipEntityType, Class sourceType, Class destType, RelationshipType relType) {

		globalRelationshipNameMap.put(relationName, new RelationshipMapping(relationName, sourceType, destType, relType));
		globalRelationshipClassMap.put(createCombinedRelationshipType(sourceType.getSimpleName(), relType.name(), destType.getSimpleName()), relationshipEntityType);
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
			properties = new LinkedHashSet<PropertyKey>();
			propertyViewMap.put(propertyView, properties);
		}

		// add all properties from set
		properties.addAll(Arrays.asList(propertySet));
	}

	// ----- searchable property map -----
	/**
	 * Registers the given set of properties of the given entity type to be stored
	 * in the given index.
	 * 
	 * @param type the entitiy type
	 * @param index the index
	 * @param keys the keys to be indexed
	 */
	public static void registerSearchablePropertySet(Class type, String index, PropertyKey... keys) {

		for (PropertyKey key : keys) {

			registerSearchableProperty(type, index, key);

		}
	}

	/**
	 * Registers the given property of the given entity type to be stored
	 * in the given index.
	 * 
	 * @param type the entitiy type
	 * @param index the index
	 * @param key the key to be indexed
	 */
	public static void registerSearchableProperty(Class type, String index, PropertyKey key) {

		Map<String, Set<PropertyKey>> searchablePropertyMapForType = getSearchablePropertyMapForType(type);
		Set<PropertyKey> searchablePropertySet                     = searchablePropertyMapForType.get(index);

		if (searchablePropertySet == null) {

			searchablePropertySet = new LinkedHashSet<PropertyKey>();

			searchablePropertyMapForType.put(index, searchablePropertySet);

		}

		key.registerSearchableProperties(searchablePropertySet);
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

					// logger.log(Level.INFO, "String {0} normalized to {1}", new Object[] { possibleEntityName, normalizedType });
					normalizedEntityNameCache.put(possibleEntityName, normalizedType);

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

				// logger.log(Level.INFO, "String {0} normalized to {1}", new Object[] { possibleEntityName, normalizedType });
				normalizedEntityNameCache.put(possibleEntityString, normalizedType);

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

	public static String createCombinedRelationshipType(String oldCombinedRelationshipType, Class newDestType) {

		String[] parts = getPartsOfCombinedRelationshipType(oldCombinedRelationshipType);

		return createCombinedRelationshipType(parts[0], parts[1], newDestType.getSimpleName());
	}

	public static String createCombinedRelationshipType(Class sourceType, RelationshipType relType, Class destType) {
		
		if (sourceType != null && relType != null && destType != null) {
			
			return createCombinedRelationshipType(sourceType.getSimpleName(), relType.name(), destType.getSimpleName());
		}
		
		return "";
	}

	public static String createCombinedRelationshipType(String sourceType, String relType, String destType) {

		return sourceType.concat(COMBINED_RELATIONSHIP_KEY_SEP).concat(relType).concat(COMBINED_RELATIONSHIP_KEY_SEP).concat(destType);

	}
	
	public static void registerConvertedProperty(PropertyKey propertyKey) {
		globalKnownPropertyKeys.add(propertyKey);
	}

	//~--- get methods ----------------------------------------------------

	public static Class getEntityClassForRawType(final String rawType) {

		String normalizedEntityName = normalizeEntityName(rawType);

		if (cachedEntities.containsKey(normalizedEntityName)) {

			return (Class) cachedEntities.get(normalizedEntityName);

		}
		
		if (reverseInterfaceMap.containsKey(normalizedEntityName)) {

			return (Class) reverseInterfaceMap.get(normalizedEntityName);

		}

		return null;
	}

	public static RelationshipMapping getNamedRelation(String relationName) {
		return globalRelationshipNameMap.get(relationName);
	}
	
	private static Class getSourceType(final String combinedRelationshipType) {

		String sourceType = getPartsOfCombinedRelationshipType(combinedRelationshipType)[0];
		Class realType  = getEntityClassForRawType(sourceType);

		return realType;
	}
	
	private static RelationshipType getRelType(final String combinedRelationshipType) {
		String relType = getPartsOfCombinedRelationshipType(combinedRelationshipType)[1];
		return DynamicRelationshipType.withName(relType);
	}

	private static Class getDestType(final String combinedRelationshipType) {

		String destType = getPartsOfCombinedRelationshipType(combinedRelationshipType)[2];
		Class realType  = getEntityClassForRawType(destType);

		return realType;
	}

	private static String[] getPartsOfCombinedRelationshipType(final String combinedRelationshipType) {
		return StringUtils.split(combinedRelationshipType, COMBINED_RELATIONSHIP_KEY_SEP);
	}

	public static Class getNamedRelationClass(String sourceType, String destType, String relType) {
		return getNamedRelationClass(createCombinedRelationshipType(sourceType, relType, destType));
	}

	public static Class getNamedRelationClass(String combinedRelationshipType) {

		Class relEntity = globalRelationshipClassMap.get(combinedRelationshipType);
		if (relEntity == null) {

			Class sourceType         = getSourceType(combinedRelationshipType);
			Class destType           = getDestType(combinedRelationshipType);
			RelationshipType relType = getRelType(combinedRelationshipType);

			relEntity = getNamedRelationClass(sourceType, destType, relType);
		}
		
		return relEntity;
	}

	public static Class getNamedRelationClass(Class sourceType, Class destType, RelationshipType relType) {

		Class namedRelationClass = null;
		Class sourceSuperClass   = sourceType;
		Class destSuperClass     = destType;

		while ((namedRelationClass == null) && sourceSuperClass != null && destSuperClass != null && !Object.class.equals(sourceSuperClass) && !Object.class.equals(destSuperClass)) {

			namedRelationClass = globalRelationshipClassMap.get(createCombinedRelationshipType(sourceSuperClass, relType, destSuperClass));

			// check interfaces of dest class
			if (namedRelationClass == null) {
				
				for(Class interfaceClass : getInterfacesForType(destSuperClass)) {
					
					namedRelationClass = globalRelationshipClassMap.get(createCombinedRelationshipType(sourceSuperClass, relType, interfaceClass));
					if(namedRelationClass != null) {
						break;
					}
				}
			}
			// do not check superclass for source type
			// sourceSuperClass = sourceSuperClass.getSuperclass();
			// one level up
			destSuperClass = destSuperClass.getSuperclass();

		}

		if (namedRelationClass != null) {

			return namedRelationClass;

		}

		return globalRelationshipClassMap.get(createCombinedRelationshipType(sourceType, relType, destType));
	}

	public static Collection<RelationshipMapping> getNamedRelations() {
		return globalRelationshipNameMap.values();
	}

	public static Set<Transformation<GraphObject>> getEntityCreationTransformations(Class type) {

		Set<Transformation<GraphObject>> transformations = new TreeSet<Transformation<GraphObject>>();
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
			viewTransformationMap = new LinkedHashMap<String, ViewTransformation>();
			viewTransformations.put(type, viewTransformationMap);
		}
		
		return viewTransformationMap;
	}
	

	// ----- property set methods -----
	public static Set<String> getPropertyViews() {

		Set<String> views = new LinkedHashSet<String>();
		
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
			properties = new LinkedHashSet<PropertyKey>();
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

		Set<PropertyValidator> validators                     = new LinkedHashSet<PropertyValidator>();
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

	public static Set<PropertyKey> getSearchableProperties(Class type, String index) {

		Set<PropertyKey> searchablePropertyMap = getSearchablePropertyMapForType(type).get(index);
		if (searchablePropertyMap == null) {

			searchablePropertyMap = new HashSet<PropertyKey>();
		}

		return searchablePropertyMap;
	}

	private static Map<String, Set<PropertyKey>> getPropertyViewMapForType(Class type) {

		Map<String, Set<PropertyKey>> propertyViewMap = globalPropertyViewMap.get(type);

		if (propertyViewMap == null) {

			propertyViewMap = new LinkedHashMap<String, Set<PropertyKey>>();

			globalPropertyViewMap.put(type, propertyViewMap);

		}

		return propertyViewMap;
	}

	private static Map<String, PropertyKey> getClassDBNamePropertyMapForType(Class type) {

		Map<String, PropertyKey> classDBNamePropertyMap = globalClassDBNamePropertyMap.get(type);

		if (classDBNamePropertyMap == null) {

			classDBNamePropertyMap = new LinkedHashMap<String, PropertyKey>();

			globalClassDBNamePropertyMap.put(type, classDBNamePropertyMap);

		}

		return classDBNamePropertyMap;
	}

	private static Map<String, PropertyKey> getClassJSNamePropertyMapForType(Class type) {

		Map<String, PropertyKey> classJSNamePropertyMap = globalClassJSNamePropertyMap.get(type);

		if (classJSNamePropertyMap == null) {

			classJSNamePropertyMap = new LinkedHashMap<String, PropertyKey>();

			globalClassJSNamePropertyMap.put(type, classJSNamePropertyMap);

		}

		return classJSNamePropertyMap;
	}

	private static Map<PropertyKey, Set<PropertyValidator>> getPropertyValidatorMapForType(Class type) {

		Map<PropertyKey, Set<PropertyValidator>> validatorMap = globalValidatorMap.get(type);

		if (validatorMap == null) {

			validatorMap = new LinkedHashMap<PropertyKey, Set<PropertyValidator>>();

			globalValidatorMap.put(type, validatorMap);

		}

		return validatorMap;
	}

	public static Map<String, Set<PropertyKey>> getSearchablePropertyMapForType(Class type) {

		Map<String, Set<PropertyKey>> searchablePropertyMap = globalSearchablePropertyMap.get(type);

		if (searchablePropertyMap == null) {

			searchablePropertyMap = new LinkedHashMap<String, Set<PropertyKey>>();

			globalSearchablePropertyMap.put(type, searchablePropertyMap);

		}

		return searchablePropertyMap;
	}

	private static Map<String, PropertyGroup> getAggregatedPropertyGroupMapForType(Class type) {

		Map<String, PropertyGroup> groupMap = globalAggregatedPropertyGroupMap.get(type);

		if (groupMap == null) {

			groupMap = new LinkedHashMap<String, PropertyGroup>();

			globalAggregatedPropertyGroupMap.put(type, groupMap);

		}

		return groupMap;
	}

	private static Map<String, PropertyGroup> getPropertyGroupMapForType(Class type) {

		Map<String, PropertyGroup> groupMap = globalPropertyGroupMap.get(type);

		if (groupMap == null) {

			groupMap = new LinkedHashMap<String, PropertyGroup>();

			globalPropertyGroupMap.put(type, groupMap);

		}

		return groupMap;
	}

	private static Set<Transformation<GraphObject>> getEntityCreationTransformationsForType(Class type) {

		Set<Transformation<GraphObject>> transformations = globalTransformationMap.get(type);

		if (transformations == null) {

			transformations = new LinkedHashSet<Transformation<GraphObject>>();

			globalTransformationMap.put(type, transformations);

		}

		return transformations;
	}
	
	public static Set<Class> getInterfacesForType(Class type) {
		
		Set<Class> interfaces = interfaceMap.get(type);
		if(interfaces == null) {
			
			interfaces = new LinkedHashSet<Class>();
			interfaceMap.put(type, interfaces);
			
			for(Class iface : type.getInterfaces()) {

				reverseInterfaceMap.put(iface.getSimpleName(), iface);
				interfaces.add(iface);
			}
		}
		
		return interfaces;
	}
	
	public static boolean isKnownProperty(final PropertyKey key) {
		return globalKnownPropertyKeys.contains(key);
	}

	public static boolean isSearchableProperty(Class type, String index, PropertyKey key) {
		
		boolean isSearchable = getSearchableProperties(type, index).contains(key);
		
		return isSearchable;
	}

	public static FactoryDefinition getFactoryDefinition() {
		return factoryDefinition;
	}
	
	public static void registerFactoryDefinition(FactoryDefinition factory) {
		factoryDefinition = factory;
	}
	
	private static <T> Map<Field, T> getFieldValuesOfType(Class<T> entityType, Object entity) {
		
		Map<Field, T> fields   = new LinkedHashMap<Field, T>();
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

		Set<Class<?>> types = new LinkedHashSet<Class<?>>();
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
}
