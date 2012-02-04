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

import java.util.*;
import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;

import org.structr.common.CaseHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.entity.DirectedRelationship.Cardinality;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.IndexNodeCommand;
import org.structr.core.node.StructrNodeFactory;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 * A global context for functional mappings between nodes and
 * relationships, property views and property validators.
 *
 * @author Christian Morgner
 */
public class EntityContext {

	public static final String GLOBAL_UNIQUENESS                                                                = "global_uniqueness_key";
	private static final Map<Class, Map<String, Class<? extends PropertyConverter>>> globalPropertyConverterMap = new LinkedHashMap<Class, Map<String, Class<? extends PropertyConverter>>>();
	private static final Map<Class, Map<String, Set<PropertyValidator>>> globalValidatorMap                     = new LinkedHashMap<Class, Map<String, Set<PropertyValidator>>>();
	private static final Map<String, Map<String, Set<String>>> globalSearchablePropertyMap                      = new LinkedHashMap<String, Map<String, Set<String>>>();
	private static final Map<Class, Map<String, Set<String>>> globalPropertyViewMap                             = new LinkedHashMap<Class, Map<String, Set<String>>>();
	private static final Map<String, Map<String, DirectedRelationship>> globalPropertyRelationshipMap           = new LinkedHashMap<String, Map<String, DirectedRelationship>>();
	private static final Map<Class, Map<String, PropertyGroup>> globalPropertyGroupMap                          = new LinkedHashMap<Class, Map<String, PropertyGroup>>();
	private static final Map<String, Map<String, DirectedRelationship>> globalEntityRelationshipMap             = new LinkedHashMap<String, Map<String, DirectedRelationship>>();
	private static final Map<Class, Map<String, Value>> globalConversionParameterMap                            = new LinkedHashMap<Class, Map<String, Value>>();
	private static final Map<Class, Set<String>> globalWriteOncePropertyMap                                     = new LinkedHashMap<Class, Set<String>>();
	private static final Map<Class, Set<String>> globalReadOnlyPropertyMap                                      = new LinkedHashMap<Class, Set<String>>();
	private static final Map<String, String> normalizedEntityNameCache                                          = new LinkedHashMap<String, String>();
	private static final Set<VetoableGraphObjectListener> modificationListeners                                 = new LinkedHashSet<VetoableGraphObjectListener>();
	private static final Map<Thread, Long> transactionKeyMap                                                    = new LinkedHashMap<Thread, Long>();
	private static final Map<Long, FrameworkException> exceptionMap                                             = new LinkedHashMap<Long, FrameworkException>();
	private static final Logger logger                                                                          = Logger.getLogger(EntityContext.class.getName());
	private static final Map<Class, Set<Transformation<AbstractNode>>> globalPostCreationTransformationMap      = new LinkedHashMap<Class, Set<Transformation<AbstractNode>>>();
	private static final EntityContextModificationListener globalModificationListener                           = new EntityContextModificationListener();

	//~--- methods --------------------------------------------------------

	public static void registerModificationListener(VetoableGraphObjectListener listener) {
		modificationListeners.add(listener);
	}

	public static void unregisterModificationListener(VetoableGraphObjectListener listener) {
		modificationListeners.remove(listener);
	}

	public static void registerPostCreationTransformation(Class type, Transformation<AbstractNode> transformation) {
		getPostCreationTransformationsForType(type).add(transformation);
	}

	public static void registerPropertyGroup(Class type, PropertyKey key, PropertyGroup propertyGroup) {
		getPropertyGroupMapForType(type).put(key.name(), propertyGroup);
	}

	// ----- property and entity relationships -----
	public static void registerPropertyRelation(Class sourceType, PropertyKey propertyKey, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality) {
		registerPropertyRelation(sourceType, propertyKey.name(), destType, relType, direction, cardinality);
	}

	public static void registerPropertyRelation(Class sourceType, String property, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality) {

		// need to set type here
		Notion objectNotion = new ObjectNotion();

		objectNotion.setType(destType);
		registerPropertyRelationInternal(sourceType.getSimpleName(), property, destType.getSimpleName(), relType, direction, cardinality, objectNotion);
	}

	public static void registerPropertyRelation(Class sourceType, PropertyKey propertyKey, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality, Notion notion) {
		registerPropertyRelation(sourceType, propertyKey.name(), destType, relType, direction, cardinality, notion);
	}

	public static void registerPropertyRelation(Class sourceType, String property, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality, Notion notion) {

		notion.setType(destType);
		registerPropertyRelationInternal(sourceType.getSimpleName(), property, destType.getSimpleName(), relType, direction, cardinality, notion);
	}

	public static void registerEntityRelation(Class sourceType, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality) {

		// need to set type here
		Notion objectNotion = new ObjectNotion();

		objectNotion.setType(destType);
		registerEntityRelationInternal(sourceType.getSimpleName(), destType.getSimpleName(), relType, direction, cardinality, objectNotion);
	}

	public static void registerEntityRelation(Class sourceType, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality, Notion notion) {

		notion.setType(destType);
		registerEntityRelationInternal(sourceType.getSimpleName(), destType.getSimpleName(), relType, direction, cardinality, notion);
	}

	// ----- property set methods -----
	public static void registerPropertySet(Class type, String propertyView, PropertyKey... propertySet) {

		Set<String> properties = getPropertySet(type, propertyView);

		for (PropertyKey property : propertySet) {

			properties.add(property.name());

		}

		// include property sets from superclass
		Class superClass = type.getSuperclass();

		while ((superClass != null) &&!superClass.equals(Object.class)) {

			Set<String> superProperties = getPropertySet(superClass, propertyView);

			properties.addAll(superProperties);

			// one level up :)
			superClass = superClass.getSuperclass();

		}
	}

	public static void clearPropertySet(Class type, String propertyView) {
		getPropertySet(type, propertyView).clear();
	}

	// ----- validator methods -----
	public static void registerPropertyValidator(Class type, PropertyKey propertyKey, PropertyValidator validatorClass) {
		registerPropertyValidator(type, propertyKey.name(), validatorClass);
	}

	public static void registerPropertyValidator(Class type, String propertyKey, PropertyValidator validator) {

		Map<String, Set<PropertyValidator>> validatorMap = getPropertyValidatorMapForType(type);

		// fetch or create validator set
		Set<PropertyValidator> validators = validatorMap.get(propertyKey);
		if(validators == null) {
			validators = new LinkedHashSet<PropertyValidator>();
			validatorMap.put(propertyKey, validators);
		}

		validators.add(validator);
	}

	// ----- PropertyConverter methods -----
	public static void registerPropertyConverter(Class type, PropertyKey propertyKey, Class<? extends PropertyConverter> propertyConverterClass) {
		registerPropertyConverter(type, propertyKey.name(), propertyConverterClass);
	}

	public static void registerPropertyConverter(Class type, PropertyKey propertyKey, Class<? extends PropertyConverter> propertyConverterClass, Value value) {
		registerPropertyConverter(type, propertyKey.name(), propertyConverterClass, value);
	}

	public static void registerPropertyConverter(Class type, String propertyKey, Class<? extends PropertyConverter> propertyConverterClass) {
		registerPropertyConverter(type, propertyKey, propertyConverterClass, null);
	}

	public static void registerPropertyConverter(Class type, String propertyKey, Class<? extends PropertyConverter> propertyConverterClass, Value value) {

		getPropertyConverterMapForType(type).put(propertyKey, propertyConverterClass);

		if (value != null) {

			getPropertyConversionParameterMapForType(type).put(propertyKey, value);

		}
	}

	// ----- read-only property map -----
	public static void registerReadOnlyProperty(Class type, String key) {
		getReadOnlyPropertySetForType(type).add(key);
	}

	// ----- searchable property map -----
	public static void registerSearchablePropertySet(Class type, String index, PropertyKey... keys) {

		for (PropertyKey key : keys) {

			registerSearchablePropertySet(type, index, key);

		}
	}

	public static void registerSearchablePropertySet(Class type, String index, PropertyKey key) {
		registerSearchablePropertySet(type, index, key.name());
	}

	public static void registerSearchablePropertySet(Class type, String index, String key) {
		registerSearchablePropertySet(type.getSimpleName(), index, key);
	}

	public static void registerSearchablePropertySet(String type, String index, String key) {

		Map<String, Set<String>> searchablePropertyMapForType = getSearchablePropertyMapForType(type);
		Set<String> searchablePropertySet                     = searchablePropertyMapForType.get(index);

		if (searchablePropertySet == null) {

			searchablePropertySet = new LinkedHashSet<String>();

			searchablePropertyMapForType.put(index, searchablePropertySet);

		}

		searchablePropertySet.add(key);
	}

	// ----- write-once property map -----
	public static void registerWriteOnceProperty(Class type, String key) {
		getWriteOncePropertySetForType(type).add(key);
	}

	// ----- private methods -----
	public static String normalizeEntityName(String possibleEntityName) {

		// CAUTION: this cache might grow to a very large size, as it
		// contains all normalized mappings for every possible
		// property key / entity name that is ever called.
		String normalizedType = normalizedEntityNameCache.get(possibleEntityName);

		if (normalizedType == null) {

			normalizedType = StringUtils.capitalize(CaseHelper.toUpperCamelCase(possibleEntityName));

			if (normalizedType.endsWith("ies")) {

				normalizedType = normalizedType.substring(0, normalizedType.length() - 3).concat("y");

			} else if (normalizedType.endsWith("s")) {

				logger.log(Level.FINEST, "Removing trailing plural 's' from type {0}", normalizedType);

				normalizedType = normalizedType.substring(0, normalizedType.length() - 1);

			}

			// logger.log(Level.INFO, "String {0} normalized to {1}", new Object[] { possibleEntityName, normalizedType });
			normalizedEntityNameCache.put(possibleEntityName, normalizedType);

		}

		return normalizedType;
	}

	private static void registerPropertyRelationInternal(String sourceType, String property, String destType, RelationshipType relType, Direction direction, Cardinality cardinality,
		Notion notion) {

		Map<String, DirectedRelationship> typeMap = getPropertyRelationshipMapForType(sourceType);

		typeMap.put(property, new DirectedRelationship(destType, relType, direction, cardinality, notion));
	}

	private static void registerEntityRelationInternal(String sourceType, String destType, RelationshipType relType, Direction direction, Cardinality cardinality, Notion notion) {

		Map<String, DirectedRelationship> typeMap = getEntityRelationshipMapForType(sourceType);

		typeMap.put(destType, new DirectedRelationship(destType, relType, direction, cardinality, notion));
	}

	public static synchronized void removeTransactionKey() {
		transactionKeyMap.remove(Thread.currentThread());
	}

	//~--- get methods ----------------------------------------------------

	public static Set<VetoableGraphObjectListener> getModificationListeners() {
		return modificationListeners;
	}

	public static Set<Transformation<AbstractNode>> getPostCreationTransformations(Class type) {

		Set<Transformation<AbstractNode>> transformations = new TreeSet<Transformation<AbstractNode>>();
		Class localType                                   = type;

		// collect for all superclasses
		while (!localType.equals(Object.class)) {

			transformations.addAll(getPostCreationTransformationsForType(localType));

			localType = localType.getSuperclass();

		}

//		return new TreeSet<Transformation<AbstractNode>>(transformations).descendingSet();
		return transformations;
	}

	// ----- property notions -----
	public static PropertyGroup getPropertyGroup(Class type, PropertyKey key) {
		return getPropertyGroup(type, key.name());
	}

	public static PropertyGroup getPropertyGroup(Class type, String key) {
		return getPropertyGroupMapForType(type).get(key);
	}

	// ----- static relationship methods -----
	public static DirectedRelationship getDirectedRelationship(Class sourceType, Class destType) {
		return getDirectedRelationship(sourceType.getSimpleName(), destType.getSimpleName());
	}

	public static DirectedRelationship getDirectedRelationship(Class sourceType, String key) {
		return getDirectedRelationship(sourceType.getSimpleName(), key);
	}

	public static DirectedRelationship getDirectedRelationship(String sourceType, String key) {

		String normalizedSourceType = normalizeEntityName(sourceType);
		DirectedRelationship rel    = null;

		// try property relations with EXACT MATCH first
		rel = getPropertyRelationshipMapForType(normalizedSourceType).get(key);

		if (rel == null) {

			// no relationship for exact match, try normalized entity name now
			rel = getEntityRelationshipMapForType(normalizedSourceType).get(normalizeEntityName(key));
		}

		return rel;
	}

	// ----- property set methods -----
	public static Set<String> getPropertySet(Class type, String propertyView) {

		Map<String, Set<String>> propertyViewMap = getPropertyViewMapForType(type);
		Set<String> propertySet                  = propertyViewMap.get(propertyView);

		if (propertySet == null) {

			propertySet = new LinkedHashSet<String>();

			propertyViewMap.put(propertyView, propertySet);

		}

		return propertySet;
	}

	public static Set<PropertyValidator> getPropertyValidators(final SecurityContext securityContext, Class type, String propertyKey) {

		Map<String, Set<PropertyValidator>> validatorMap = null;
		Set<PropertyValidator> validators                = null;
		Class localType                                  = type;

		// try all superclasses
		while ((validators == null) &&!localType.equals(Object.class)) {

			validatorMap = getPropertyValidatorMapForType(localType);
			validators   = validatorMap.get(propertyKey);

//                      logger.log(Level.INFO, "Validator class {0} found for type {1}", new Object[] { clazz != null ? clazz.getSimpleName() : "null", localType } );
			// one level up :)
			localType = localType.getSuperclass();

		}

		return validators;
	}

	public static PropertyConverter getPropertyConverter(final SecurityContext securityContext, Class type, String propertyKey) {

		Map<String, Class<? extends PropertyConverter>> converterMap = null;
		PropertyConverter propertyConverter                          = null;
		Class localType                                              = type;
		Class clazz                                                  = null;

		while ((clazz == null) &&!localType.equals(Object.class)) {

			converterMap = getPropertyConverterMapForType(localType);
			clazz        = converterMap.get(propertyKey);

//                      logger.log(Level.INFO, "Converter class {0} found for type {1}", new Object[] { clazz != null ? clazz.getSimpleName() : "null", localType } );
			localType = localType.getSuperclass();

		}

		if (clazz != null) {

			try {

				propertyConverter = (PropertyConverter) clazz.newInstance();

				propertyConverter.setSecurityContext(securityContext);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Unable to instantiate property PropertyConverter {0}: {1}", new Object[] { clazz.getName(), t.getMessage() });
			}

		}

		return propertyConverter;
	}

	public static Value getPropertyConversionParameter(Class type, String propertyKey) {

		Map<String, Value> conversionParameterMap = null;
		Class localType                           = type;
		Value value                               = null;

		while ((value == null) &&!localType.equals(Object.class)) {

			conversionParameterMap = getPropertyConversionParameterMapForType(localType);
			value                  = conversionParameterMap.get(propertyKey);

//                      logger.log(Level.INFO, "Conversion parameter value {0} found for type {1}", new Object[] { value != null ? value.getClass().getSimpleName() : "null", localType } );
			localType = localType.getSuperclass();

		}

		return value;
	}

	public static Set<String> getSearchableProperties(Class type, String index) {
		return getSearchableProperties(type.getSimpleName(), index);
	}

	public static Set<String> getSearchableProperties(String type, String index) {
		return getSearchablePropertyMapForType(type).get(index);
	}

	private static Map<String, DirectedRelationship> getPropertyRelationshipMapForType(String sourceType) {

		Map<String, DirectedRelationship> typeMap = globalPropertyRelationshipMap.get(normalizeEntityName(sourceType));

		if (typeMap == null) {

			typeMap = new LinkedHashMap<String, DirectedRelationship>();

			globalPropertyRelationshipMap.put(normalizeEntityName(sourceType), typeMap);

		}

		return (typeMap);
	}

	private static Map<String, DirectedRelationship> getEntityRelationshipMapForType(String sourceType) {

		Map<String, DirectedRelationship> typeMap = globalEntityRelationshipMap.get(normalizeEntityName(sourceType));

		if (typeMap == null) {

			typeMap = new LinkedHashMap<String, DirectedRelationship>();

			globalEntityRelationshipMap.put(normalizeEntityName(sourceType), typeMap);

		}

		return (typeMap);
	}

	private static Map<String, Set<String>> getPropertyViewMapForType(Class type) {

		Map<String, Set<String>> propertyViewMap = globalPropertyViewMap.get(type);

		if (propertyViewMap == null) {

			propertyViewMap = new LinkedHashMap<String, Set<String>>();

			globalPropertyViewMap.put(type, propertyViewMap);

		}

		return propertyViewMap;
	}

	private static Map<String, Set<PropertyValidator>> getPropertyValidatorMapForType(Class type) {

		Map<String, Set<PropertyValidator>> validatorMap = globalValidatorMap.get(type);

		if (validatorMap == null) {

			validatorMap = new LinkedHashMap<String, Set<PropertyValidator>>();
			globalValidatorMap.put(type, validatorMap);

		}

		return validatorMap;
	}

	private static Map<String, Class<? extends PropertyConverter>> getPropertyConverterMapForType(Class type) {

		Map<String, Class<? extends PropertyConverter>> PropertyConverterMap = globalPropertyConverterMap.get(type);

		if (PropertyConverterMap == null) {

			PropertyConverterMap = new LinkedHashMap<String, Class<? extends PropertyConverter>>();

			globalPropertyConverterMap.put(type, PropertyConverterMap);

		}

		return PropertyConverterMap;
	}

	private static Map<String, Value> getPropertyConversionParameterMapForType(Class type) {

		Map<String, Value> conversionParameterMap = globalConversionParameterMap.get(type);

		if (conversionParameterMap == null) {

			conversionParameterMap = new LinkedHashMap<String, Value>();

			globalConversionParameterMap.put(type, conversionParameterMap);

		}

		return conversionParameterMap;
	}

	private static Set<String> getReadOnlyPropertySetForType(Class type) {

		Set<String> readOnlyPropertySet = globalReadOnlyPropertyMap.get(type);

		if (readOnlyPropertySet == null) {

			readOnlyPropertySet = new LinkedHashSet<String>();

			globalReadOnlyPropertyMap.put(type, readOnlyPropertySet);

		}

		return readOnlyPropertySet;
	}

	private static Set<String> getWriteOncePropertySetForType(Class type) {

		Set<String> writeOncePropertySet = globalWriteOncePropertyMap.get(type);

		if (writeOncePropertySet == null) {

			writeOncePropertySet = new LinkedHashSet<String>();

			globalWriteOncePropertyMap.put(type, writeOncePropertySet);

		}

		return writeOncePropertySet;
	}

	private static Map<String, Set<String>> getSearchablePropertyMapForType(String sourceType) {

		Map<String, Set<String>> searchablePropertyMap = globalSearchablePropertyMap.get(normalizeEntityName(sourceType));

		if (searchablePropertyMap == null) {

			searchablePropertyMap = new LinkedHashMap<String, Set<String>>();

			globalSearchablePropertyMap.put(normalizeEntityName(sourceType), searchablePropertyMap);

		}

		return searchablePropertyMap;
	}

	private static Map<String, PropertyGroup> getPropertyGroupMapForType(Class type) {

		Map<String, PropertyGroup> groupMap = globalPropertyGroupMap.get(type);

		if (groupMap == null) {

			groupMap = new LinkedHashMap<String, PropertyGroup>();

			globalPropertyGroupMap.put(type, groupMap);

		}

		return groupMap;
	}

	private static Set<Transformation<AbstractNode>> getPostCreationTransformationsForType(Class type) {

		Set<Transformation<AbstractNode>> transformations = globalPostCreationTransformationMap.get(type);

		if (transformations == null) {

			transformations = new LinkedHashSet<Transformation<AbstractNode>>();

			globalPostCreationTransformationMap.put(type, transformations);

		}

		return transformations;
	}

	public static EntityContextModificationListener getTransactionEventHandler() {
		return globalModificationListener;
	}

	public static synchronized FrameworkException getFrameworkException(Long transactionKey) {
		return exceptionMap.get(transactionKey);
	}

	public static boolean isReadOnlyProperty(Class type, String key) {

		boolean isReadOnly = getReadOnlyPropertySetForType(type).contains(key);
		Class localType    = type;

		// try all superclasses
		while (!isReadOnly &&!localType.equals(Object.class)) {

			isReadOnly = getReadOnlyPropertySetForType(localType).contains(key);

			// one level up :)
			localType = localType.getSuperclass();

		}

		return isReadOnly;
	}

	public static boolean isSearchableProperty(Class type, String index, PropertyKey key) {
		return isSearchableProperty(type, index, key.name());
	}

	public static boolean isSearchableProperty(Class type, String index, String key) {
		return isSearchableProperty(type.getSimpleName(), index, key);
	}

	public static boolean isSearchableProperty(String type, String index, String key) {
		return getSearchablePropertyMapForType(normalizeEntityName(type)).containsKey(key);
	}

	public static boolean isWriteOnceProperty(Class type, String key) {

		boolean isWriteOnce = getWriteOncePropertySetForType(type).contains(key);
		Class localType     = type;

		// try all superclasses
		while (!isWriteOnce &&!localType.equals(Object.class)) {

			isWriteOnce = getWriteOncePropertySetForType(localType).contains(key);

			// one level up :)
			localType = localType.getSuperclass();

		}

		return isWriteOnce;
	}

	//~--- set methods ----------------------------------------------------

	public static synchronized void setTransactionKey(Long transactionKey) {
		transactionKeyMap.put(Thread.currentThread(), transactionKey);
	}

	//~--- inner classes --------------------------------------------------

	// <editor-fold defaultstate="collapsed" desc="EntityContextModificationListener">
	public static class EntityContextModificationListener implements VetoableGraphObjectListener, TransactionEventHandler<Long> {

		
		// ----- interface TransactionEventHandler -----
		@Override
		public Long beforeCommit(TransactionData data) throws Exception {
			
			Command indexCommand = Services.command(SecurityContext.getSuperUserInstance(), IndexNodeCommand.class);

			long transactionKey = transactionKeyMap.get(Thread.currentThread());

			try {

				ErrorBuffer errorBuffer                          = new ErrorBuffer();
				boolean hasError                                 = false;
				Map<Node, Map<String, Object>> removedProperties = new LinkedHashMap<Node, Map<String, Object>>();
				SecurityContext securityContext                  = SecurityContext.getSuperUserInstance();
				StructrNodeFactory factory                       = new StructrNodeFactory(securityContext);
				Set<AbstractNode> modifiedNodes                  = new LinkedHashSet<AbstractNode>();
				Set<AbstractNode> createdNodes                   = new LinkedHashSet<AbstractNode>();
				Set<StructrRelationship> createdRels             = new LinkedHashSet<StructrRelationship>();
				Set<StructrRelationship> deletedRels             = new LinkedHashSet<StructrRelationship>();

				// 0: notify listeners of beginning commit
				begin(securityContext, transactionKey, errorBuffer);

				// 1: collect properties of deleted nodes
				for (PropertyEntry<Node> entry : data.removedNodeProperties()) {

					Node node                       = entry.entity();
					Map<String, Object> propertyMap = removedProperties.get(node);

					if (propertyMap == null) {

						propertyMap = new LinkedHashMap<String, Object>();

						removedProperties.put(node, propertyMap);

					}

					propertyMap.put(entry.key(), entry.previouslyCommitedValue());

				}

				// 2: notify listeners of node creation (so the modifications can later be tracked)
				for (Node node : data.createdNodes()) {

					AbstractNode entity = factory.createNode(securityContext, node);

					hasError |= graphObjectCreated(securityContext, transactionKey, errorBuffer, entity);

					createdNodes.add(entity);

				}

				// 3: notify listeners of relationship creation
				for (Relationship rel : data.createdRelationships()) {

					StructrRelationship relationship = new StructrRelationship(securityContext, rel);

					hasError |= relationshipCreated(securityContext, transactionKey, errorBuffer, relationship);

					createdRels.add(relationship);

				}

				// 4: notify listeners of relationship deletion
				for (Relationship rel : data.deletedRelationships()) {

					StructrRelationship relationship = new StructrRelationship(securityContext, rel);

					hasError |= relationshipDeleted(securityContext, transactionKey, errorBuffer, relationship);

					deletedRels.add(relationship);

				}

				// 5: notify listeners of node deletion
				for (Node node : data.deletedNodes()) {

					hasError |= graphObjectDeleted(securityContext, transactionKey, errorBuffer, node.getId(), removedProperties.get(node));

				}

				// 6: validate property modifications and
				// notify listeners of property modifications
				for (PropertyEntry<Node> entry : data.assignedNodeProperties()) {

					AbstractNode entity = factory.createNode(securityContext, entry.entity());
					String key          = entry.key();
					Object value        = entry.value();

					// iterate over validators
					Set<PropertyValidator> validators = EntityContext.getPropertyValidators(securityContext, entity.getClass(), key);
					if (validators != null) {
						for(PropertyValidator validator : validators) {
							hasError |= !(validator.isValid(entity, key, value, errorBuffer));
						}

					}

					hasError |= propertyModified(securityContext, transactionKey, errorBuffer, entity, key, entry.previouslyCommitedValue(), value);

					// after successful validation, add node to index to make uniqueness constraints work
					indexCommand.execute(entity, key);
					modifiedNodes.add(entity);

				}

				// 7: notify listeners of modified nodes (to check for non-existing properties etc)
				for (AbstractNode node : modifiedNodes) {

					// only send UPDATE if node was not created in this transaction
					if (!createdNodes.contains(node)) {

						hasError |= graphObjectModified(securityContext, transactionKey, errorBuffer, node);

					}
				}

				// notify listeners of commit
				hasError |= commit(securityContext, transactionKey, errorBuffer);

				if (hasError) {

					throw new FrameworkException(422, errorBuffer);

				}

			} catch (FrameworkException fex) {

				exceptionMap.put(transactionKey, fex);

				throw new IllegalStateException("Rollback");
			}

			return transactionKey;
		}

		@Override
		public void afterCommit(TransactionData data, Long transactionKey) {

//
		}

		@Override
		public void afterRollback(TransactionData data, Long transactionKey) {

			Throwable t = exceptionMap.get(transactionKey);

			if (t != null) {

				// thow
				throw new IllegalArgumentException(t);
			}
		}

		@Override
		public boolean begin(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.begin(securityContext, transactionKey, errorBuffer);

			}

			return hasError;
		}

		@Override
		public boolean commit(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.commit(securityContext, transactionKey, errorBuffer);

			}

			return hasError;
		}

		@Override
		public boolean rollback(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.rollback(securityContext, transactionKey, errorBuffer);

			}

			return hasError;
		}

		@Override
		public boolean propertyModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, AbstractNode entity, String key, Object oldValue, Object newValue) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.propertyModified(securityContext, transactionKey, errorBuffer, entity, key, oldValue, newValue);

			}

			return hasError;
		}

		@Override
		public boolean relationshipCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, StructrRelationship relationship) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.relationshipCreated(securityContext, transactionKey, errorBuffer, relationship);

			}

			return hasError;
		}

		@Override
		public boolean relationshipDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, StructrRelationship relationship) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.relationshipDeleted(securityContext, transactionKey, errorBuffer, relationship);

			}

			return hasError;
		}

		@Override
		public boolean graphObjectCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.graphObjectCreated(securityContext, transactionKey, errorBuffer, graphObject);

			}

			return hasError;
		}

		@Override
		public boolean graphObjectModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.graphObjectModified(securityContext, transactionKey, errorBuffer, graphObject);

			}

			return hasError;
		}

		@Override
		public boolean wasVisited(List<GraphObject> traversedNodes, long transactionKey, ErrorBuffer errorBuffer, SecurityContext securityContext) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.wasVisited(traversedNodes, transactionKey, errorBuffer, securityContext);

			}

			return hasError;
		}

		@Override
		public boolean graphObjectDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, long id, Map<String, Object> properties) {

			boolean hasError = false;

			for (VetoableGraphObjectListener listener : modificationListeners) {

				hasError |= listener.graphObjectDeleted(securityContext, transactionKey, errorBuffer, id, properties);

			}

			return hasError;
		}
	}

	// </editor-fold>
}
