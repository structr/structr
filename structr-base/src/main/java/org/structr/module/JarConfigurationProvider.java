/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.module;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Agent;
import org.structr.api.service.LicenseManager;
import org.structr.api.service.Service;
import org.structr.common.*;
import org.structr.core.*;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The module service main class.
 */
public class JarConfigurationProvider implements ConfigurationProvider {

	private static final Logger logger = LoggerFactory.getLogger(JarConfigurationProvider.class.getName());

	public static final URI StaticSchemaRootURI      = URI.create("https://structr.org/v2.0/#");
	public static final String DYNAMIC_TYPES_PACKAGE = "org.structr.dynamic";

	private static final Set<String> coreModules                                                   = new HashSet<>(Arrays.asList("core", "rest", "ui"));

	private final Map<String, Class<? extends RelationshipInterface>> relationshipEntityClassCache = new ConcurrentHashMap<>(1000);
	private final Map<String, Class<? extends NodeInterface>> nodeEntityClassCache                 = new ConcurrentHashMap(1000);
	private final Map<String, Class<? extends Agent>> agentClassCache                              = new ConcurrentHashMap<>(100);

	private final Set<String> agentPackages                                                        = new LinkedHashSet<>();
 	private final Set<String> nodeEntityPackages                                                   = new LinkedHashSet<>();
 	private final Set<String> relationshipPackages                                                 = new LinkedHashSet<>();

	private final Map<String, Class> combinedTypeRelationClassCache                                = new ConcurrentHashMap<>(100);
	private final Map<String, Set<Class>> interfaceCache                                           = new ConcurrentHashMap<>(2000);
	private final Map<String, StructrModule> modules                                               = new ConcurrentHashMap<>(100);

	private final String fileSep                                                                   = System.getProperty("file.separator");
	private final String pathSep                                                                   = System.getProperty("path.separator");
	private final String fileSepEscaped                                                            = fileSep.replaceAll("\\\\", "\\\\\\\\");
	private final String testClassesDir                                                            = fileSep.concat("test-classes");
	private final String classesDir                                                                = fileSep.concat("classes");

	private final Map<String, Map<String, Set<PropertyKey>>> globalPropertyViewMap                 = new ConcurrentHashMap<>(2000);
	private final Map<String, Map<PropertyKey, Set<PropertyValidator>>> globalValidatorMap         = new ConcurrentHashMap<>(100);
	private final Map<String, Map<String, PropertyKey>> globalClassDBNamePropertyMap               = new ConcurrentHashMap<>(2000);
	private final Map<String, Map<String, PropertyKey>> globalBuiltinClassDBNamePropertyMap        = new ConcurrentHashMap<>(2000);
	private final Map<String, Map<String, PropertyKey>> globalClassJSNamePropertyMap               = new ConcurrentHashMap<>(2000);
	private final Map<String, Map<String, PropertyKey>> globalBuiltinClassJSNamePropertyMap        = new ConcurrentHashMap<>(2000);
	private final Map<String, Map<String, PropertyGroup>> globalAggregatedPropertyGroupMap         = new ConcurrentHashMap<>(100);
	private final Map<String, Map<String, PropertyGroup>> globalPropertyGroupMap                   = new ConcurrentHashMap<>(100);
	private final Map<String, Set<Transformation<GraphObject>>> globalTransformationMap            = new ConcurrentHashMap<>(100);
	private final Map<String, Map<String, Method>> exportedMethodMap                               = new ConcurrentHashMap<>(100);
	private final Map<Class, Set<Class>> interfaceMap                                              = new ConcurrentHashMap<>(2000);
	private final Map<String, Class> reverseInterfaceMap                                           = new ConcurrentHashMap<>(5000);

	private final Set<PropertyKey> globalKnownPropertyKeys                                         = new LinkedHashSet<>();
	private final Set<String> dynamicViews                                                         = new LinkedHashSet<>();

	private FactoryDefinition factoryDefinition                                                    = new DefaultFactoryDefinition();
	private LicenseManager licenseManager                                                          = null;

	// ----- interface ConfigurationProvider -----
	@Override
	public void initialize(final LicenseManager licenseManager) {

		this.licenseManager = licenseManager;

		scanResources();
	}

	@Override
	public void shutdown() {
	}

	@Override
	public Map<String, Class<? extends Agent>> getAgents() {
		return agentClassCache;
	}

	@Override
	public Map<String, Class<? extends NodeInterface>> getNodeEntities() {

		synchronized (SchemaService.class) {
			return nodeEntityClassCache;
		}
	}

	@Override
	public Map<String, Class<? extends RelationshipInterface>> getRelationshipEntities() {

		synchronized (SchemaService.class) {
			return relationshipEntityClassCache;
		}
	}

	@Override
	public Set<Class> getClassesForInterface(final String simpleName) {

		synchronized (SchemaService.class) {
			return interfaceCache.get(simpleName);
		}
	}

	@Override
	public Class getNodeEntityClass(final String simpleName) {

		Class nodeEntityClass = GenericNode.class;

		if ((simpleName != null) && (!simpleName.isEmpty())) {

			synchronized (SchemaService.class) {

				nodeEntityClass = nodeEntityClassCache.get(simpleName);

				if (nodeEntityClass == null) {

					for (String possiblePath : nodeEntityPackages) {

						if (possiblePath != null) {

							try {

								Class nodeClass = Class.forName(possiblePath + "." + simpleName);

								if (!Modifier.isAbstract(nodeClass.getModifiers())) {

									nodeEntityClassCache.put(simpleName, nodeClass);
									nodeEntityClass = nodeClass;

									// first match wins
									break;

								}

							} catch (ClassNotFoundException ex) {}
						}
					}
				}
			}
		}

		return nodeEntityClass;

	}

	@Override
	public Class getRelationshipEntityClass(final String name) {

		Class relationClass = AbstractRelationship.class;

		if ((name != null) && (name.length() > 0)) {

			synchronized (SchemaService.class) {

				relationClass = relationshipEntityClassCache.get(name);

				if (relationClass == null) {

					for (String possiblePath : relationshipPackages) {

						if (possiblePath != null) {

							try {

								Class nodeClass = Class.forName(possiblePath + "." + name);

								if (!Modifier.isAbstract(nodeClass.getModifiers())) {

									relationshipEntityClassCache.put(name, nodeClass);

									// first match wins
									return nodeClass;

								}

							} catch (ClassNotFoundException ex) {

								// ignore
							}
						}
					}
				}
			}
		}

		return relationClass;

	}

	public Class<? extends Agent> getAgentClass(final String name) {

		Class agentClass = null;

		if ((name != null) && (name.length() > 0)) {

			agentClass = agentClassCache.get(name);

			if (agentClass == null) {

				for (String possiblePath : agentPackages) {

					if (possiblePath != null) {

						try {

							Class nodeClass = Class.forName(possiblePath + "." + name);

							agentClassCache.put(name, nodeClass);

							// first match wins
							return nodeClass;

						} catch (ClassNotFoundException ex) {

							// ignore
						}

					}

				}

			}

		}

		return agentClass;

	}

	@Override
	public Map<String, Class> getInterfaces() {
		return reverseInterfaceMap;
	}

	@Override
	public void setRelationClassForCombinedType(final String combinedType, final Class clazz) {
		combinedTypeRelationClassCache.put(combinedType, clazz);
	}

	@Override
	public void setRelationClassForCombinedType(final String sourceType, final String relType, final String targetType, final Class clazz) {
		combinedTypeRelationClassCache.put(getCombinedType(sourceType, relType, targetType), clazz);
	}

	private Class getRelationClassForCombinedType(final String combinedType) {

		Class cachedRelationClass = combinedTypeRelationClassCache.get(combinedType);

		if (cachedRelationClass != null) {
			return cachedRelationClass;
		}

		return null;
	}

	@Override
	public Class getRelationClassForCombinedType(final String sourceTypeName, final String relType, final String targetTypeName) {

		if (sourceTypeName == null || relType == null || targetTypeName == null) {
			return null;
		}

		String combinedType
			= sourceTypeName
			.concat(DefaultFactoryDefinition.COMBINED_RELATIONSHIP_KEY_SEP)
			.concat(relType)
			.concat(DefaultFactoryDefinition.COMBINED_RELATIONSHIP_KEY_SEP)
			.concat(targetTypeName);

		Class cachedRelationClass = getRelationClassForCombinedType(combinedType);

		if (cachedRelationClass != null) {
			return cachedRelationClass;
		}

		return findNearestMatchingRelationClass(sourceTypeName, relType, targetTypeName);
	}

	/**
	 * Return a list of all relation entity classes filtered by relationship
	 * type.
	 *
	 * @param relType
	 * @return classes
	 */
	private List<Class<? extends RelationshipInterface>> getRelationClassCandidatesForRelType(final String relType) {

		List<Class<? extends RelationshipInterface>> candidates = new ArrayList();

		for (final Class<? extends RelationshipInterface> candidate : getRelationshipEntities().values()) {

			Relation rel = instantiate(candidate);

			if (rel == null) {
				continue;
			}

			if (rel.name().equals(relType)) {
				candidates.add(candidate);
			}

		}

		return candidates;

	}

	/**
	 * Find the most specialized relation class matching the given
	 * parameters.
	 *
	 * If no direct match is found (source and target type are equal), we
	 * count the levels of inheritance, including interfaces.
	 *
	 * @param sourceTypeName
	 * @param relType
	 * @param targetTypeName
	 * @return class
	 */
	private Class findNearestMatchingRelationClass(final String sourceTypeName, final String relType, final String targetTypeName) {

		final Class sourceType               = getNodeEntityClass(sourceTypeName);
		final Class targetType               = getNodeEntityClass(targetTypeName);
		final Map<Integer, Class> candidates = new TreeMap<>();

		for (final Class candidate : getRelationClassCandidatesForRelType(relType)) {

			final Relation rel = instantiate(candidate);
			final int distance = getDistance(rel.getSourceType(), sourceType, -1) + getDistance(rel.getTargetType(), targetType, -1);

			if (distance >= 2000) {
				candidates.put(distance - 2000, candidate);
			}

		}

		if (candidates.isEmpty()) {
			return null;

		} else {

			final Entry<Integer, Class> candidateEntry = candidates.entrySet().iterator().next();
			final Class c = candidateEntry.getValue();

			combinedTypeRelationClassCache.put(getCombinedType(sourceTypeName, relType, targetTypeName), c);

			return c;
		}
	}

	private int getDistance(final Class candidateType, final Class type, int distance) {

		if (distance >= 1000) {
			return distance;
		}

		distance++;

		// Just in case...
		if (type == null) {
			return Integer.MIN_VALUE;
		}

		// Abort if type is Object.class here
		if (type.equals(Object.class)) {
			return Integer.MIN_VALUE;
		}

		// Check direct equality
		if (type.equals(candidateType)) {
			return distance + 1000;
		}

		// Abort here if type is NodeInterface.
		if (type.equals(NodeInterface.class)) {
			return Integer.MIN_VALUE;
		}

		// Relation candidate's source and target types must be superclasses or interfaces of the given relationship
		if (!(candidateType.isAssignableFrom(type))) {
			return Integer.MIN_VALUE;
		}

		distance++;

		// Test source's interfaces against target class
		Class[] interfaces = type.getInterfaces();

		for (final Class iface : interfaces) {

			if (iface.equals(candidateType)) {
				return distance + 1000;
			}
		}

		distance++;

		final Class superClass = type.getSuperclass();
		if (superClass != null) {

			final int d = getDistance(candidateType, superClass, distance);
			if (d >= 1000) {

				return d;
			}
		}

		return distance;
	}

	@Override
	public Map<String, Method> getAnnotatedMethods(Class entityType, Class annotationType) {

		final Map<String, Method> methods = new HashMap<>();
		final Set<Class<?>> allTypes      = getAllTypes(entityType);

		for (final Class<?> type : allTypes) {

			for (Method method : type.getDeclaredMethods()) {

				if (method.getAnnotation(annotationType) != null) {

					methods.put(method.getName(), method);
				}
			}
		}

		return methods;
	}

	@Override
	public void unregisterEntityType(final Class oldType) {

		synchronized (SchemaService.class) {

			final String simpleName = oldType.getSimpleName();
			final String fqcn       = oldType.getName();

			nodeEntityClassCache.remove(simpleName);
			relationshipEntityClassCache.remove(simpleName);

			nodeEntityPackages.remove(fqcn);
			relationshipPackages.remove(fqcn);

			globalPropertyViewMap.remove(fqcn);
			globalClassDBNamePropertyMap.remove(fqcn);
			globalClassJSNamePropertyMap.remove(fqcn);

			interfaceMap.remove(oldType);

			// clear all
			combinedTypeRelationClassCache.clear();

			// clear interfaceCache manually..
			for (final Set<Class> classes : interfaceCache.values()) {

				if (classes.contains(oldType)) {
					classes.remove(oldType);
				}
			}
		}
	}

	@Override
	public void registerEntityType(final Class type) {

		// moved here from scanEntity, no reason to have this in a separate
		// method requiring two different calls instead of one
		final String simpleName = type.getSimpleName();
		final String fqcn       = type.getName();

		// do not register types that match org.structr.*Mixin (helpers)
		if (fqcn.startsWith("org.structr.") && simpleName.endsWith("Mixin")) {
			return;
		}

		if (AbstractNode.class.isAssignableFrom(type)) {

			nodeEntityClassCache.put(simpleName, type);
			nodeEntityPackages.add(fqcn.substring(0, fqcn.lastIndexOf(".")));
			globalPropertyViewMap.remove(fqcn);
		}

		if (AbstractRelationship.class.isAssignableFrom(type)) {

			relationshipEntityClassCache.put(simpleName, type);
			relationshipPackages.add(fqcn.substring(0, fqcn.lastIndexOf(".")));
			globalPropertyViewMap.remove(fqcn);
		}

		// interface that extends NodeInterface, must be stored
		if (type.isInterface() && GraphObject.class.isAssignableFrom(type)) {

			reverseInterfaceMap.putIfAbsent(type.getName(), type);
		}

		for (final Class interfaceClass : type.getInterfaces()) {

			final String interfaceName     = interfaceClass.getSimpleName();
			Set<Class> classesForInterface = interfaceCache.get(interfaceName);

			if (classesForInterface == null) {

				classesForInterface = new LinkedHashSet<>();

				interfaceCache.put(interfaceName, classesForInterface);
			}

			classesForInterface.add(type);

			// recurse for interfaces
			registerEntityType(interfaceClass);
		}

		try {

			final Map<Field, PropertyKey> allProperties = getFieldValuesOfType(PropertyKey.class, type);
			final Map<Field, View> views = getFieldValuesOfType(View.class, type);

			for (final Map.Entry<Field, PropertyKey> entry : allProperties.entrySet()) {

				final PropertyKey propertyKey = entry.getValue();
				final Field field             = entry.getKey();
				final Class declaringClass    = field.getDeclaringClass();

				if (declaringClass != null) {

					propertyKey.setDeclaringClass(declaringClass);
					registerProperty(declaringClass, propertyKey);
				}

				registerProperty(type, propertyKey);
			}

			for (final Map.Entry<Field, View> entry : views.entrySet()) {

				final Field field          = entry.getKey();
				final View view            = entry.getValue();
				final PropertyKey[] keys   = view.properties();
				final Class declaringClass = field.getDeclaringClass();

				// Register views and properties
				// Overwrite the view definition of superclasses for non-builtin views
				final boolean overwrite = type.equals(declaringClass)
					&& !( view.name().equals(PropertyView.All)
					|| view.name().equals(PropertyView.Custom)
					|| view.name().equals(PropertyView.Html)
					|| view.name().equals(PropertyView.Public)
					|| view.name().equals(PropertyView.Private)
					|| view.name().equals(PropertyView.Protected)
					|| view.name().equals(PropertyView.Ui)
					|| view.name().equals(SchemaRelationshipNode.exportView.name())
					|| view.name().equals(SchemaRelationshipNode.schemaView.name())
				);

				registerPropertySet(type, view.name(), overwrite, keys);

				for (final PropertyKey propertyKey : keys) {

					// replace field in any other view of this type (not superclass!)
					for (final Map.Entry<Field, View> other : views.entrySet()) {

						final View otherView = other.getValue();

						final Set<PropertyKey> otherKeys = getPropertyViewMapForType(type).get(otherView.name());
						if (otherKeys != null && otherKeys.contains(propertyKey)) {

							replaceKeyInSet(otherKeys, propertyKey);
						}
					}
				}
			}

		} catch (Throwable t) {

			logger.warn("Unable to register type {}, missing module?", type.getSimpleName());
			logger.debug("Unable to register type.", t);

			// remove already registered types in case of error
			unregisterEntityType(type);
		}

		Map<String, Method> typeMethods = exportedMethodMap.get(fqcn);
		if (typeMethods == null) {

			typeMethods = new HashMap<>();
			exportedMethodMap.put(fqcn, typeMethods);
		}

		typeMethods.putAll(getAnnotatedMethods(type, Export.class));

		// extract interfaces for later use
		getInterfacesForType(type);
	}

	/**
	 * Register a transformation that will be applied to every newly created
	 * entity of a given type.
	 *
	 * @param type the type of the entities for which the transformation
	 * should be applied
	 * @param transformation the transformation to apply on every entity
	 */
	@Override
	public void registerEntityCreationTransformation(Class type, Transformation<GraphObject> transformation) {

		final Set<Transformation<GraphObject>> transformations = getEntityCreationTransformationsForType(type);
		if (!transformations.contains(transformation)) {

			transformations.add(transformation);
		}
	}

	@Override
	public Set<Class> getInterfacesForType(Class type) {

		Set<Class> interfaces = interfaceMap.get(type);
		if (interfaces == null) {

			interfaces = new LinkedHashSet<>();
			interfaceMap.put(type, interfaces);

			for (Class iface : type.getInterfaces()) {

				interfaces.add(iface);
			}
		}

		return interfaces;
	}

	@Override
	public Map<String, Method> getExportedMethodsForType(Class type) {
		return exportedMethodMap.get(type.getName());
	}

	@Override
	public boolean isKnownProperty(final PropertyKey key) {
		return globalKnownPropertyKeys.contains(key);
	}

	@Override
	public FactoryDefinition getFactoryDefinition() {
		return factoryDefinition;
	}

	@Override
	public void registerFactoryDefinition(FactoryDefinition factory) {
		factoryDefinition = factory;
	}

	/**
	 * Registers a property group for the given key of the given entity
	 * type. A property group can be used to combine a set of properties
	 * into an object.
	 *
	 * @param type the type of the entities for which the property group
	 * should be registered
	 * @param key the property key under which the property group should be
	 * visible
	 * @param propertyGroup the property group
	 */
	@Override
	public void registerPropertyGroup(Class type, PropertyKey key, PropertyGroup propertyGroup) {
		getPropertyGroupMapForType(type).put(key.dbName(), propertyGroup);
	}

	@Override
	public void registerConvertedProperty(PropertyKey propertyKey) {
		globalKnownPropertyKeys.add(propertyKey);
	}

	@Override
	public synchronized Set<Transformation<GraphObject>> getEntityCreationTransformations(Class type) {

		Set<Transformation<GraphObject>> transformations = new TreeSet<>();
		Class localType = type;

		// collect for all superclasses
		while (localType != null && !localType.equals(Object.class)) {

			transformations.addAll(getEntityCreationTransformationsForType(localType));

			localType = localType.getSuperclass();

		}

		return transformations;
	}

	@Override
	public PropertyGroup getPropertyGroup(Class type, PropertyKey key) {
		return getPropertyGroup(type, key.dbName());
	}

	@Override
	public PropertyGroup getPropertyGroup(Class type, String key) {

		PropertyGroup group = getAggregatedPropertyGroupMapForType(type).get(key);
		if (group == null) {

			Class localType = type;

			while (group == null && localType != null && !localType.equals(Object.class)) {

				group = getPropertyGroupMapForType(localType).get(key);

				if (group == null) {

					// try interfaces as well
					for (Class interfaceClass : getInterfacesForType(localType)) {

						group = getPropertyGroupMapForType(interfaceClass).get(key);
						if (group != null) {
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

	@Override
	public Set<String> getPropertyViews() {

		Set<String> views = new LinkedHashSet<>();

		// add all existing views
		for (Map<String, Set<PropertyKey>> view : globalPropertyViewMap.values()) {
			views.addAll(view.keySet());
		}

		// merge dynamic views in as well
		views.addAll(dynamicViews);

		return Collections.unmodifiableSet(views);
	}

	@Override
	public Set<String> getPropertyViewsForType(final Class type) {

		final Map<String, Set<PropertyKey>> map = getPropertyViewMapForType(type);
		if (map != null) {

			return map.keySet();
		}

		return Collections.emptySet();
	}

	@Override
	public void registerDynamicViews(final Set<String> dynamicViews) {
		this.dynamicViews.clear();
		this.dynamicViews.addAll(dynamicViews);
	}

	@Override
	public boolean hasView(final Class type, final String propertyView) {

		final Map<String, Set<PropertyKey>> propertyViewMap = getPropertyViewMapForType(type);
		final Set<PropertyKey> properties = propertyViewMap.get(propertyView);

		return (properties != null);
	}

	@Override
	public Set<PropertyKey> getPropertySet(Class type, String propertyView) {

		Map<String, Set<PropertyKey>> propertyViewMap = getPropertyViewMapForType(type);
		Set<PropertyKey> properties = propertyViewMap.get(propertyView);

		if (properties == null) {
			properties = new LinkedHashSet<>();
		}

		// read-only
		return Collections.unmodifiableSet(properties);
	}

	/**
	 * Registers the given set of property keys for the view with name
	 * <code>propertyView</code> and the given prefix of entities with the
	 * given type.
	 *
	 * @param type the type of the entities for which the view will be
	 * registered
	 * @param propertyView the name of the property view for which the
	 * property set will be registered
	 * @param propertySet the set of property keys to register for the given
	 * view
	 * @param forceOverwrite if true, remove properties from existing views
	 * that are not contained in the new property set
	 */
	public void registerPropertySet(final Class type, final String propertyView, final boolean forceOverwrite, PropertyKey... propertySet) {

		final Map<String, Set<PropertyKey>> propertyViewMap = getPropertyViewMapForType(type);
		Set<PropertyKey> properties = propertyViewMap.get(propertyView);

		if (forceOverwrite || properties == null) {
			properties = new LinkedHashSet<>();
			propertyViewMap.put(propertyView, properties);
		}

		// allow properties to override existing ones as they
		// are most likely from a more concrete class.
		for (final PropertyKey key : propertySet) {

			// property keys are referenced by their names,
			// that's why we seemingly remove the existing
			// key, but the set does not differentiate
			// between different keys
			if (properties.contains(key)) {
				properties.remove(key);
			}

			properties.add(key);
		}
	}

	/**
	 * Registers the given set of property keys for the view with name
	 * <code>propertyView</code> and the given prefix of entities with the
	 * given type.
	 *
	 * @param type the type of the entities for which the view will be
	 * registered
	 * @param propertyView the name of the property view for which the
	 * property set will be registered
	 * @param propertySet the set of property keys to register for the given
	 * view
	 */
	@Override
	public void registerPropertySet(final Class type, final String propertyView, final PropertyKey... propertySet) {
		registerPropertySet(type, propertyView, false, propertySet);
	}

	@Override
	public void registerPropertySet(final Class type, final String propertyView, final String propertyName) {
		this.registerPropertySet(type, propertyView, this.getPropertyKeyForJSONName(type, propertyName));
	}

	@Override
	public PropertyKey getPropertyKeyForDatabaseName(Class type, String dbName) {
		return getPropertyKeyForDatabaseName(type, dbName, true);
	}

	@Override
	public PropertyKey getPropertyKeyForDatabaseName(Class type, String dbName, boolean createGeneric) {

		final Map<String, PropertyKey> classDBNamePropertyMap = getClassDBNamePropertyMapForType(type);
		PropertyKey key = classDBNamePropertyMap.get(dbName);

		if (key == null) {

			// first try: uuid
			if (GraphObject.id.dbName().equals(dbName)) {
				return GraphObject.id;
			}

			if (createGeneric) {
				key = new GenericProperty(dbName);
			}
		}

		return key;
	}

	@Override
	public PropertyKey getPropertyKeyForJSONName(Class type, String jsonName) {
		return getPropertyKeyForJSONName(type, jsonName, true);
	}

	@Override
	public PropertyKey getPropertyKeyForJSONName(Class type, String jsonName, boolean createIfNotFound) {

		if (jsonName == null) {
			return null;
		}

		Map<String, PropertyKey> classJSNamePropertyMap = getClassJSNamePropertyMapForType(type);
		PropertyKey key = classJSNamePropertyMap.get(jsonName);

		if (key == null) {

			// first try: uuid
			if (GraphObject.id.dbName().equals(jsonName)) {

				return GraphObject.id;
			}

			if (createIfNotFound) {

				key = new GenericProperty(jsonName);
			}
		}

		return key;
	}

	@Override
	public void setPropertyKeyForJSONName(final Class type, final String jsonName, final PropertyKey key) {

		final Map<String, PropertyKey> classJSNamePropertyMap = getClassJSNamePropertyMapForType(type);

		classJSNamePropertyMap.put(jsonName, key);
	}

	@Override
	public Set<PropertyValidator> getPropertyValidators(final SecurityContext securityContext, Class type, PropertyKey propertyKey) {

		Set<PropertyValidator> validators = new LinkedHashSet<>();
		Map<PropertyKey, Set<PropertyValidator>> validatorMap = null;
		Class localType = type;

		// try all superclasses
		while (localType != null && !localType.equals(Object.class)) {

			validatorMap = getPropertyValidatorMapForType(localType);

			Set<PropertyValidator> classValidators = validatorMap.get(propertyKey);
			if (classValidators != null) {
				validators.addAll(validatorMap.get(propertyKey));
			}

			// try converters from interfaces as well
			for (Class interfaceClass : getInterfacesForType(localType)) {
				Set<PropertyValidator> interfaceValidators = getPropertyValidatorMapForType(interfaceClass).get(propertyKey);
				if (interfaceValidators != null) {
					validators.addAll(interfaceValidators);
				}
			}

			// one level up :)
			localType = localType.getSuperclass();

		}

		return validators;
	}

	@Override
	public void registerProperty(Class type, PropertyKey propertyKey) {

		final Map<String, PropertyKey> dbNamePropertyMap = getClassDBNamePropertyMapForType(type);

		// backup key (if present and either not dynamic or part of builtin schema)
		final PropertyKey existingDBNamePropertyKey = dbNamePropertyMap.get(propertyKey.dbName());

		if (existingDBNamePropertyKey != null && (!existingDBNamePropertyKey.isDynamic() || existingDBNamePropertyKey.isPartOfBuiltInSchema())) {
			getBuiltinClassDBNamePropertyMapForType(type).put(propertyKey.dbName(), existingDBNamePropertyKey);
		}

		dbNamePropertyMap.put(propertyKey.dbName(), propertyKey);


		final Map<String, PropertyKey> jsonNamePropertyMap = getClassJSNamePropertyMapForType(type);

		// backup key (if present and either not dynamic or part of builtin schema)
		final PropertyKey existingJSONNamePropertyKey = jsonNamePropertyMap.get(propertyKey.jsonName());

		if (existingJSONNamePropertyKey != null && (!existingJSONNamePropertyKey.isDynamic() || existingJSONNamePropertyKey.isPartOfBuiltInSchema())) {
			getBuiltinClassJSNamePropertyMapForType(type).put(propertyKey.jsonName(), existingJSONNamePropertyKey);
		}

		jsonNamePropertyMap.put(propertyKey.jsonName(), propertyKey);

		registerPropertySet(type, PropertyView.All, propertyKey);

		// inform property key of its registration
		propertyKey.registrationCallback(type);
	}

	@Override
	public void unregisterProperty(Class type, PropertyKey propertyKey) {

		getClassDBNamePropertyMapForType(type).remove(propertyKey.dbName());

		Map<String, PropertyKey> backupdbNamePropertyMap = getBuiltinClassDBNamePropertyMapForType(type);
		if (backupdbNamePropertyMap.containsKey(propertyKey.dbName())) {
			// restore builtin property
			getClassDBNamePropertyMapForType(type).put(propertyKey.dbName(), backupdbNamePropertyMap.get(propertyKey.dbName()));
		}

		getClassJSNamePropertyMapForType(type).remove(propertyKey.jsonName());

		Map<String, PropertyKey> backupjsonNamePropertyMap = getBuiltinClassDBNamePropertyMapForType(type);
		if (backupjsonNamePropertyMap.containsKey(propertyKey.jsonName())) {
			// restore builtin property
			getClassJSNamePropertyMapForType(type).put(propertyKey.jsonName(), backupjsonNamePropertyMap.get(propertyKey.jsonName()));
		}
	}

	@Override
	public void registerDynamicProperty(Class type, PropertyKey propertyKey) {

		synchronized (SchemaService.class) {

			final String typeName = type.getName();

			registerProperty(type, propertyKey);

			// scan all existing classes and find all classes that have the given type as a supertype
			for (final Class possibleSubclass : nodeEntityClassCache.values()) {

				// need to compare strings not classes here..
				for (final Class supertype : getAllTypes(possibleSubclass)) {

					if (supertype.getName().equals(typeName)) {

						registerProperty(possibleSubclass, propertyKey);
						registerPropertySet(possibleSubclass, PropertyView.Ui, propertyKey);
					}
				}
			}
		}
	}

	@Override
	public Map<String, StructrModule> getModules() {
		return modules;
	}

	@Override
	public Map<String, Map<String, PropertyKey>> getTypeAndPropertyMapping() {
		return Collections.unmodifiableMap(globalClassJSNamePropertyMap);
	}

	// ----- private methods -----
	private void scanResources() {

		Set<String> resourcePaths = getResourcesToScan();
		for (String resourcePath : resourcePaths) {

			scanResource(resourcePath);
		}

		logger.info("{} JARs scanned", resourcePaths.size());

	}

	private void scanResource(final String resourceName) {

		try {

			final StructrModuleInfo module = loadResource(resourceName);
			if (module != null) {

				importResource(module);

			} else {

				logger.warn("Module was null!");
			}

		} catch (IOException ignore) {}

	}

	private void importResource(final StructrModuleInfo module) throws IOException {

		final Set<String> classes = module.getClasses();

		for (final String name : classes) {

			String className = StringUtils.removeStart(name, ".");

			try {

				// instantiate class..
				final Class clazz   = Class.forName(className);
				final int modifiers = clazz.getModifiers();

				// register node entity classes
				if (NodeInterface.class.isAssignableFrom(clazz)) {

					registerEntityType(clazz);
				}

				// register entity classes
				if (AbstractRelationship.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

					registerEntityType(clazz);
				}

				// register services
				if (Service.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

					Services.getInstance().registerServiceClass(clazz);
				}

				// register agents
				if (Agent.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

					final String simpleName = clazz.getSimpleName();
					final String fullName   = clazz.getName();

					agentClassCache.put(simpleName, clazz);
					agentPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));
				}

				// register modules
				if (StructrModule.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

					try {

						// we need to make sure that a module is initialized exactly once
						final StructrModule structrModule = (StructrModule) clazz.getDeclaredConstructor().newInstance();
						final String moduleName = structrModule.getName();

						if (!modules.containsKey(moduleName)) {

							structrModule.registerModuleFunctions(licenseManager);

							if (coreModules.contains(moduleName) || licenseManager == null || licenseManager.isModuleLicensed(moduleName)) {

								modules.put(moduleName, structrModule);
								logger.info("Activating module {}", moduleName);

								structrModule.onLoad(licenseManager);
							}
						}

					} catch (Throwable t) {

						// log only errors from internal classes
						if (className.startsWith("org.structr.")) {

							logger.warn("Unable to instantiate module " + clazz.getName(), t);
						}
					}
				}

			} catch (Throwable t) {
				logger.warn("Error trying to load class {}: {}",  className, t.getMessage());
			}
		}
	}

	private StructrModuleInfo loadResource(String resource) throws IOException {

		// create module
		final StructrModuleInfo ret   = new StructrModuleInfo(resource);
		final Set<String> classes = ret.getClasses();

		if (resource.endsWith(".jar") || resource.endsWith(".war")) {

			try (final JarFile jarFile   = new JarFile(new File(resource), true)) {

				final Manifest manifest = jarFile.getManifest();
				if (manifest != null) {

					final Attributes attrs  = manifest.getAttributes("Structr");
					if (attrs != null) {

						final String name = attrs.getValue("Structr-Module-Name");

						// only scan and load modules that are licensed
						if (name != null) {

							if (licenseManager == null || licenseManager.isModuleLicensed(name)) {

								for (final Enumeration<? extends JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {

									final JarEntry entry = entries.nextElement();
									final String entryName = entry.getName();

									if (entryName.endsWith(".class")) {

										// cat entry > /dev/null (necessary to get signers below)
										IOUtils.copy(jarFile.getInputStream(entry), new ByteArrayOutputStream(65535));

										// verify module
										if (licenseManager == null || licenseManager.isValid(entry.getCodeSigners())) {

											final String fileEntry = entry.getName().replaceAll("[/]+", ".");
											final String fqcn      = fileEntry.substring(0, fileEntry.length() - 6);

											// add class entry to Module
											classes.add(fqcn);

											if (licenseManager != null) {
												// store licensing information
												licenseManager.addLicensedClass(fqcn);
											}
										}
									}
								}

							} else {

								// module is not licensed, only load functions as unlicensed

								for (final Enumeration<? extends JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {

									final JarEntry entry = entries.nextElement();
									final String entryName = entry.getName();

									if (entryName.endsWith(".class")) {

										// cat entry > /dev/null (necessary to get signers below)
										IOUtils.copy(jarFile.getInputStream(entry), new ByteArrayOutputStream(65535));

										// verify module
										if (licenseManager == null || licenseManager.isValid(entry.getCodeSigners())) {

											final String fileEntry = entry.getName().replaceAll("[/]+", ".");
											final String fqcn      = fileEntry.substring(0, fileEntry.length() - 6);

											try {

												final Class clazz   = Class.forName(fqcn);
												final int modifiers = clazz.getModifiers();

												// register entity classes
												if (StructrModule.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

													// we need to make sure that a module is initialized exactly once
													final StructrModule structrModule = (StructrModule) clazz.getDeclaredConstructor().newInstance();

													structrModule.registerModuleFunctions(licenseManager);

												}

											} catch (Throwable t) {
												logger.warn("Error trying to load class {}: {}",  fqcn, t.getMessage());
											}
										}
									}
								}
							}
						}
					}
				}
			}

		} else if (resource.endsWith(classesDir)) {

			// this is for testing only!
			addClassesRecursively(new File(resource), classesDir, classes);

		} else if (resource.endsWith(testClassesDir)) {

			// this is for testing only!
			addClassesRecursively(new File(resource), testClassesDir, classes);
		}

		return ret;
	}

	private void addClassesRecursively(final File dir, final String prefix, final Set<String> classes) {

		if (dir == null) {
			return;
		}

		int prefixLen = prefix.length();
		File[] files = dir.listFiles();

		if (files == null) {
			return;
		}

		for (final File file : files) {

			if (file.isDirectory()) {

				addClassesRecursively(file, prefix, classes);

			} else {

				try {

					String fileEntry = file.getAbsolutePath();

					if (fileEntry.endsWith(".class")) {

						fileEntry = fileEntry.substring(0, fileEntry.length() - 6);
						fileEntry = fileEntry.substring(fileEntry.indexOf(prefix) + prefixLen);
						fileEntry = fileEntry.replaceAll("[".concat(fileSepEscaped).concat("]+"), ".");

						if (fileEntry.startsWith(".")) {
							fileEntry = fileEntry.substring(1);
						}

						classes.add(fileEntry);
					}

				} catch (Throwable t) {
					// ignore
					logger.warn("", t);
				}

			}

		}

	}

	private Relation instantiate(final Class clazz) {

		try {

			return (Relation) clazz.getDeclaredConstructor().newInstance();

		} catch (Throwable t) {
			// ignore
		}

		return null;
	}

	private String getCombinedType(final String sourceType, final String relType, final String targetType) {

		return sourceType
			.concat(DefaultFactoryDefinition.COMBINED_RELATIONSHIP_KEY_SEP)
			.concat(relType)
			.concat(DefaultFactoryDefinition.COMBINED_RELATIONSHIP_KEY_SEP)
			.concat(targetType);
	}

	/**
	 * Scans the class path and returns a Set containing all structr
	 * modules.
	 *
	 * @return a Set of active module names
	 */
	private Set<String> getResourcesToScan() {

		final String classPath    = System.getProperty("java.class.path");
		final Set<String> modules = new TreeSet<>();
		final Pattern pattern     = Pattern.compile(".*(structr).*(war|jar)");
		final Matcher matcher     = pattern.matcher("");

		for (final String jarPath : classPath.split("[".concat(pathSep).concat("]+"))) {

			final String lowerPath = jarPath.toLowerCase();

			if (lowerPath.endsWith(classesDir) || lowerPath.endsWith(testClassesDir)) {

				modules.add(jarPath);

			} else {

				final String moduleName = lowerPath.substring(lowerPath.lastIndexOf(pathSep) + 1);

				matcher.reset(moduleName);

				if (matcher.matches()) {

					modules.add(jarPath);
				}

			}

		}

		for (final String resource : Services.getInstance().getResources()) {

			final String lowerResource = resource.toLowerCase();

			if (lowerResource.endsWith(".jar") || lowerResource.endsWith(".war")) {

				modules.add(resource);
			}

		}

		return modules;
	}

	private <T> Map<Field, T> getFieldValuesOfType(final Class<T> fieldType, final Class entityType) {

		final Map<Field, T> fields   = new LinkedHashMap<>();
		final Set<Class<?>> allTypes = getAllTypes(entityType);

		for (final Class<?> type : allTypes) {

			for (final Field field : type.getDeclaredFields()) {

				// only use static fields, because field.get(null) will throw a NPE on non-static fields
				if (fieldType.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {

					try {

						// ensure access
						field.setAccessible(true);

						// fetch value
						final T value = (T) field.get(null);
						if (value != null) {

							fields.put(field, value);
						}

					} catch (Throwable t) {
						// ignore
					}
				}
			}
		}

		return fields;
	}

	private Set<Class<?>> getAllTypes(final Class<?> type) {

		final List<Class<?>> types = new LinkedList<>();
		Class localType = type;

		do {

			collectAllInterfaces(localType, types);
			types.add(localType);

			localType = localType.getSuperclass();

		} while (localType != null && !localType.equals(Object.class));

		Collections.reverse(types);

		return new LinkedHashSet<>(types);
	}

	private void collectAllInterfaces(final Class<?> type, final List<Class<?>> interfaces) {

		if (interfaces.contains(type)) {
			return;
		}

		for (final Class iface : type.getInterfaces()) {

			collectAllInterfaces(iface, interfaces);
			interfaces.add(iface);
		}
	}

	private Map<String, Set<PropertyKey>> getPropertyViewMapForType(final Class type) {

		Map<String, Set<PropertyKey>> propertyViewMap = globalPropertyViewMap.get(type.getName());
		if (propertyViewMap == null) {

			propertyViewMap = new LinkedHashMap<>();

			globalPropertyViewMap.put(type.getName(), propertyViewMap);

		}

		return propertyViewMap;
	}

	private Map<String, PropertyKey> getClassDBNamePropertyMapForType(final Class type) {

		Map<String, PropertyKey> classDBNamePropertyMap = globalClassDBNamePropertyMap.get(type.getName());
		if (classDBNamePropertyMap == null) {

			classDBNamePropertyMap = new LinkedHashMap<>();

			globalClassDBNamePropertyMap.put(type.getName(), classDBNamePropertyMap);

		}

		return classDBNamePropertyMap;
	}

	private Map<String, PropertyKey> getBuiltinClassDBNamePropertyMapForType(final Class type) {

		Map<String, PropertyKey> classDBNamePropertyMap = globalBuiltinClassDBNamePropertyMap.get(type.getName());
		if (classDBNamePropertyMap == null) {

			classDBNamePropertyMap = new LinkedHashMap<>();

			globalBuiltinClassDBNamePropertyMap.put(type.getName(), classDBNamePropertyMap);

		}

		return classDBNamePropertyMap;
	}

	private Map<String, PropertyKey> getClassJSNamePropertyMapForType(final Class type) {

		Map<String, PropertyKey> classJSNamePropertyMap = globalClassJSNamePropertyMap.get(type.getName());
		if (classJSNamePropertyMap == null) {

			classJSNamePropertyMap = new LinkedHashMap<>();

			globalClassJSNamePropertyMap.put(type.getName(), classJSNamePropertyMap);

		}

		return classJSNamePropertyMap;
	}

	private Map<String, PropertyKey> getBuiltinClassJSNamePropertyMapForType(final Class type) {

		Map<String, PropertyKey> classJSNamePropertyMap = globalBuiltinClassJSNamePropertyMap.get(type.getName());
		if (classJSNamePropertyMap == null) {

			classJSNamePropertyMap = new LinkedHashMap<>();

			globalBuiltinClassJSNamePropertyMap.put(type.getName(), classJSNamePropertyMap);

		}

		return classJSNamePropertyMap;
	}

	private Map<PropertyKey, Set<PropertyValidator>> getPropertyValidatorMapForType(final Class type) {

		Map<PropertyKey, Set<PropertyValidator>> validatorMap = globalValidatorMap.get(type.getName());
		if (validatorMap == null) {

			validatorMap = new LinkedHashMap<>();

			globalValidatorMap.put(type.getName(), validatorMap);

		}

		return validatorMap;
	}

	private Map<String, PropertyGroup> getAggregatedPropertyGroupMapForType(final Class type) {

		Map<String, PropertyGroup> groupMap = globalAggregatedPropertyGroupMap.get(type.getName());
		if (groupMap == null) {

			groupMap = new LinkedHashMap<>();

			globalAggregatedPropertyGroupMap.put(type.getName(), groupMap);

		}

		return groupMap;
	}

	private Map<String, PropertyGroup> getPropertyGroupMapForType(final Class type) {

		Map<String, PropertyGroup> groupMap = globalPropertyGroupMap.get(type.getName());
		if (groupMap == null) {

			groupMap = new LinkedHashMap<>();

			globalPropertyGroupMap.put(type.getName(), groupMap);

		}

		return groupMap;
	}

	private Set<Transformation<GraphObject>> getEntityCreationTransformationsForType(final Class type) {

		final String name = type.getName();

		Set<Transformation<GraphObject>> transformations = globalTransformationMap.get(name);
		if (transformations == null) {

			transformations = new LinkedHashSet<>();

			globalTransformationMap.put(name, transformations);
		}

		return transformations;
	}

	private void replaceKeyInSet(final Set<PropertyKey> set, final PropertyKey key) {

		final List<PropertyKey> list = new LinkedList<>(set);

		set.clear();

		for (final PropertyKey existingKey : list) {

			if (existingKey.equals(key)) {

				set.add(key);

			} else {

				set.add(existingKey);
			}
		}
	}

	public void printCacheStats() {

 		logger.info("{}", relationshipEntityClassCache.size());
 		logger.info("{}", nodeEntityClassCache.size());
 		logger.info("{}", nodeEntityPackages.size());
 		logger.info("{}", relationshipPackages.size());
		logger.info("{}", combinedTypeRelationClassCache.size());
 		logger.info("{}", interfaceCache.size());
 		logger.info("{}", globalPropertyViewMap.size());
		logger.info("{}", globalValidatorMap.size());
 		logger.info("{}", globalClassDBNamePropertyMap.size());
 		logger.info("{}", globalClassJSNamePropertyMap.size());
		logger.info("{}", globalAggregatedPropertyGroupMap.size());
		logger.info("{}", globalPropertyGroupMap.size());
		logger.info("{}", globalTransformationMap.size());
		logger.info("{}", exportedMethodMap.size());
		logger.info("{}", interfaceMap.size());
	 	logger.info("{}", reverseInterfaceMap.size());
		logger.info("{}", globalKnownPropertyKeys.size());
		logger.info("{}", dynamicViews.size());
	}
}
