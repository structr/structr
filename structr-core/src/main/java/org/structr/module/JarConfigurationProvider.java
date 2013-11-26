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


package org.structr.module;

import org.structr.agent.Agent;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericNode;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.lang.StringUtils;
import org.structr.common.DefaultFactoryDefinition;
import org.structr.common.FactoryDefinition;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.core.*;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;

//~--- classes ----------------------------------------------------------------

/**
 * The module service main class.
 * 
 * @author Christian Morgner
 */
public class JarConfigurationProvider implements ConfigurationProvider {

	private static final Logger logger                                                             = Logger.getLogger(JarConfigurationProvider.class.getName());
	
	private final Map<String, Class<? extends RelationshipInterface>> relationshipEntityClassCache = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<String, Class<? extends NodeInterface>> nodeEntityClassCache                 = new ConcurrentHashMap(100, 0.9f, 8);
	private final Map<String, Class<? extends Agent>> agentClassCache	                       = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Set<String> nodeEntityPackages				                       = new LinkedHashSet<>();
	private final Set<String> relationshipPackages				                       = new LinkedHashSet<>();
	private final Map<String, Class> combinedTypeRelationClassCache		                       = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<String, Set<Class>> interfaceCache			                       = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Set<String> agentPackages					                       = new LinkedHashSet<>();
	private final String fileSep						                       = System.getProperty("file.separator");
	private final String fileSepEscaped					                       = fileSep.replaceAll("\\\\", "\\\\\\\\");	// ....
	private final String pathSep						                       = System.getProperty("path.separator");
	private final String testClassesDir					                       = fileSep.concat("test-classes");
	private final String classesDir						                       = fileSep.concat("classes");
	 
	private final Map<Class, Map<PropertyKey, Set<PropertyValidator>>> globalValidatorMap          = new LinkedHashMap<>();
	private final Map<Class, Map<String, Set<PropertyKey>>> globalPropertyViewMap                  = new LinkedHashMap<>();
	private final Map<Class, Map<String, PropertyKey>> globalClassDBNamePropertyMap                = new LinkedHashMap<>();
	private final Map<Class, Map<String, PropertyKey>> globalClassJSNamePropertyMap                = new LinkedHashMap<>();
	private final Map<Class, Map<String, PropertyGroup>> globalAggregatedPropertyGroupMap          = new LinkedHashMap<>();
	private final Map<Class, Map<String, PropertyGroup>> globalPropertyGroupMap                    = new LinkedHashMap<>();
	private final Map<Class, Map<String, ViewTransformation>> viewTransformations                  = new LinkedHashMap<>();
	private final Map<Class, Set<Transformation<GraphObject>>> globalTransformationMap             = new LinkedHashMap<>();
	private final Map<Class, Set<Method>> exportedMethodMap                                        = new LinkedHashMap<>();
	private final Map<Class, Set<Class>> interfaceMap                                              = new LinkedHashMap<>();
	private final Map<String, Class> reverseInterfaceMap                                           = new LinkedHashMap<>();
	private final Set<PropertyKey> globalKnownPropertyKeys                                         = new LinkedHashSet<>();
        
	private FactoryDefinition factoryDefinition                                                    = new DefaultFactoryDefinition();

	// ----- interface Configuration -----
	@Override
	public void initialize() {
		scanResources();
	}

	@Override
	public void shutdown() {

		/* do not clear caches
		nodeEntityClassCache.clear();
		combinedTypeRelationClassCache.clear();
		relationshipEntityClassCache.clear();
		agentClassCache.clear();
		*/
	}

	@Override
	public Map<String, Class<? extends Agent>> getAgents() {
		return agentClassCache;
	}

	@Override
	public Map<String, Class<? extends NodeInterface>> getNodeEntities() {
		return nodeEntityClassCache;
	}

	@Override
	public Map<String, Class<? extends RelationshipInterface>> getRelationshipEntities() {
		return relationshipEntityClassCache;
	}

	@Override
	public Set<Class> getClassesForInterface(final String simpleName) {
		return interfaceCache.get(simpleName);
	}

	@Override
	public Class getNodeEntityClass(final String name) {

		Class nodeEntityClass = GenericNode.class;

		if ((name != null) && (!name.isEmpty())) {

			nodeEntityClass = nodeEntityClassCache.get(name);

			if (nodeEntityClass == null) {

				for (String possiblePath : nodeEntityPackages) {

					if (possiblePath != null) {

						try {

							Class nodeClass = Class.forName(possiblePath + "." + name);

							if (!Modifier.isAbstract(nodeClass.getModifiers())) {

								nodeEntityClassCache.put(name, nodeClass);

								// first match wins
								break;

							}

						} catch (ClassNotFoundException ex) {

							// ignore
						}

					}

				}

			}

		}

		return nodeEntityClass;

	}

	@Override
	public Class getRelationshipEntityClass(final String name) {

		Class relationClass = AbstractNode.class;

		if ((name != null) && (name.length() > 0)) {

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
	public void setRelationClassForCombinedType(final String combinedType, final Class clazz) {
		combinedTypeRelationClassCache.put(combinedType, clazz);
	}
	
	@Override
	public void setRelationClassForCombinedType(final String sourceType, final String relType, final String targetType, final Class clazz) {
		combinedTypeRelationClassCache.put(getCombinedType(sourceType, relType, targetType), clazz);
	}

	public Class getRelationClassForCombinedType(final String combinedType) {
		
		Class cachedRelationClass = combinedTypeRelationClassCache.get(combinedType);
		
		if (cachedRelationClass != null) {
			return cachedRelationClass;
		}
		
		return null;
	}
		
	@Override
	public Class getRelationClassForCombinedType(final String sourceType, final String relType, final String targetType) {
		
		if (sourceType == null || relType == null || targetType == null) {
			return null;
		}
		
		String combinedType =
			
			sourceType
			.concat(DefaultFactoryDefinition.COMBINED_RELATIONSHIP_KEY_SEP)
			.concat(relType)
			.concat(DefaultFactoryDefinition.COMBINED_RELATIONSHIP_KEY_SEP)
			.concat(targetType);
		
		Class cachedRelationClass = getRelationClassForCombinedType(combinedType);
		
		if (cachedRelationClass != null) {
			return cachedRelationClass;
		}
		
		for (final Class candidate : getRelationshipEntities().values()) {

			final Relation rel = instantiate(candidate);
			if (rel != null) {
				
				final String sourceTypeName = rel.getSourceType().getSimpleName();
				final String relTypeName    = rel.name();
				final String targetTypeName = rel.getTargetType().getSimpleName();

				if (sourceType.equals(sourceTypeName) && relType.equals(relTypeName) && targetType.equals(targetTypeName)) {
					
					combinedType = getCombinedType(sourceType, relType, targetType);
					combinedTypeRelationClassCache.put(combinedType, candidate);
					
					return candidate;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public Set<Method> getAnnotatedMethods(Class entityType, Class annotationType) {
		
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

	@Override
	public void registerEntityType(final Class type) {

		// moved here from scanEntity, no reason to have this in a separate
		// method requiring two different calls instead of one
		int modifiers = type.getModifiers();
		if (!Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)) {
			
			try {
				
				Object entity                         = type.newInstance();
				Map<Field, PropertyKey> allProperties = getFieldValuesOfType(PropertyKey.class, entity);
				Map<Field, View> views                = getFieldValuesOfType(View.class, entity);
				Class entityType                      = entity.getClass();

				for (Map.Entry<Field, PropertyKey> entry : allProperties.entrySet()) {

					PropertyKey propertyKey = entry.getValue();
					Field field             = entry.getKey();
					Class declaringClass    = field.getDeclaringClass();

					if (declaringClass != null) {

						propertyKey.setDeclaringClass(declaringClass);
						registerProperty(declaringClass, propertyKey);

					}

					registerProperty(entityType, propertyKey);
				}

				for (Map.Entry<Field, View> entry : views.entrySet()) {

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
	
	@Override
	public void registerProperty(Class type, PropertyKey propertyKey) {
		
		getClassDBNamePropertyMapForType(type).put(propertyKey.dbName(),   propertyKey);
		getClassJSNamePropertyMapForType(type).put(propertyKey.jsonName(), propertyKey);
		
		registerPropertySet(type, PropertyView.All, propertyKey);
		
		// inform property key of its registration
		propertyKey.registrationCallback(type);
	}

	/**
	 * Registers the given set of property keys for the view with name <code>propertyView</code>
	 * and the given prefix of entities with the given type.
	 * 
	 * @param type the type of the entities for which the view will be registered
	 * @param propertyView the name of the property view for which the property set will be registered
	 * @param viewPrefix a string that will be prepended to all property keys in this view
	 * @param propertySet the set of property keys to register for the given view
	 */
	@Override
	public void registerPropertySet(Class type, String propertyView, PropertyKey... propertySet) {

		Map<String, Set<PropertyKey>> propertyViewMap = getPropertyViewMapForType(type);
		Set<PropertyKey> properties                   = propertyViewMap.get(propertyView);
		
		if (properties == null) {
			properties = new LinkedHashSet<>();
			propertyViewMap.put(propertyView, properties);
		}

		// add all properties from set
		properties.addAll(Arrays.asList(propertySet));
	}
	
	/**
	 * Register a transformation that will be applied to every newly created entity of a given type.
	 * 
	 * @param type the type of the entities for which the transformation should be applied
	 * @param transformation the transformation to apply on every entity
	 */
	@Override
	public void registerEntityCreationTransformation(Class type, Transformation<GraphObject> transformation) {
		getEntityCreationTransformationsForType(type).add(transformation);
	}

	@Override
	public Set<Class> getInterfacesForType(Class type) {
		
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
	
	@Override
	public Set<Method> getExportedMethodsForType(Class type) {
		return exportedMethodMap.get(type);
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
	 * Registers a property group for the given key of the given entity type. A property group can be
	 * used to combine a set of properties into an object. {@see PropertyGroup}
	 * 
	 * @param type the type of the entities for which the property group should be registered
	 * @param key the property key under which the property group should be visible
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
	public Set<Transformation<GraphObject>> getEntityCreationTransformations(Class type) {

		Set<Transformation<GraphObject>> transformations = new TreeSet<>();
		Class localType                                  = type;

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

	@Override
	public void registerViewTransformation(Class type, String view, ViewTransformation transformation) {
		getViewTransformationMapForType(type).put(view, transformation);
	}
	
	@Override
	public ViewTransformation getViewTransformation(Class type, String view) {
		return getViewTransformationMapForType(type).get(view);
	}

	@Override
	public Set<String> getPropertyViews() {

		Set<String> views = new LinkedHashSet<>();
		
		// add all existing views
		for (Map<String, Set<PropertyKey>> view : globalPropertyViewMap.values()) {
			views.addAll(view.keySet());
		}
		
		return Collections.unmodifiableSet(views);
	}
	
	@Override
	public Set<PropertyKey> getPropertySet(Class type, String propertyView) {

		Map<String, Set<PropertyKey>> propertyViewMap = getPropertyViewMapForType(type);
		Set<PropertyKey> properties                   = propertyViewMap.get(propertyView);

		if (properties == null) {
			properties = new LinkedHashSet<>();
		}
		
		// read-only
		return Collections.unmodifiableSet(properties);
	}
	
	@Override
	public PropertyKey getPropertyKeyForDatabaseName(Class type, String dbName) {
		return getPropertyKeyForDatabaseName(type, dbName, true);
	}
	
	@Override
	public PropertyKey getPropertyKeyForDatabaseName(Class type, String dbName, boolean createGeneric) {

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

	@Override
	public Set<PropertyValidator> getPropertyValidators(final SecurityContext securityContext, Class type, PropertyKey propertyKey) {

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
	
	// ----- private methods -----
	private void scanResources() {

		Set<String> resourcePaths = getResourcesToScan();

		for (String resourcePath : resourcePaths) {

			scanResource(resourcePath);
		}

		logger.log(Level.INFO, "{0} JARs scanned", resourcePaths.size());

	}

	private void scanResource(String resourceName) {

		try {

			Module module = loadResource(resourceName);

			if (module != null) {

				importResource(module);
				
			} else {

				logger.log(Level.WARNING, "Module was null!");
			}

		} catch (IOException ioex) {

			logger.log(Level.WARNING, "Error loading module {0}: {1}", new Object[] { resourceName, ioex });
			ioex.printStackTrace();

		}

	}

	private void importResource(Module module) throws IOException {

		final ConfigurationProvider configuration = StructrApp.getConfiguration();
		final Set<String> classes         = module.getClasses();

		for (final String name : classes) {
			
			String className = StringUtils.removeStart(name, ".");

			logger.log(Level.FINE, "Instantiating class {0} ", className);

			try {

				// instantiate class..
				Class clazz = Class.forName(className);

				logger.log(Level.FINE, "Class {0} instantiated: {1}", new Object[] { className, clazz });

				if (!Modifier.isAbstract(clazz.getModifiers())) {

					// register node entity classes
					if (AbstractNode.class.isAssignableFrom(clazz)) {

						configuration.registerEntityType(clazz);
						
						String simpleName = clazz.getSimpleName();
						String fullName   = clazz.getName();

						nodeEntityClassCache.put(simpleName, clazz);
						nodeEntityPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

						for (Class interfaceClass : clazz.getInterfaces()) {

							String interfaceName           = interfaceClass.getSimpleName();
							Set<Class> classesForInterface = interfaceCache.get(interfaceName);

							if (classesForInterface == null) {

								classesForInterface = new LinkedHashSet<>();

								interfaceCache.put(interfaceName, classesForInterface);

							}

							classesForInterface.add(clazz);

						}

					}

					// register entity classes
					if (AbstractRelationship.class.isAssignableFrom(clazz)) {

						configuration.registerEntityType(clazz);

						String simpleName = clazz.getSimpleName();
						String fullName   = clazz.getName();

						relationshipEntityClassCache.put(simpleName, clazz);
						relationshipPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

						for (Class interfaceClass : clazz.getInterfaces()) {

							String interfaceName           = interfaceClass.getSimpleName();
							Set<Class> classesForInterface = interfaceCache.get(interfaceName);

							if (classesForInterface == null) {

								classesForInterface = new LinkedHashSet<>();

								interfaceCache.put(interfaceName, classesForInterface);

							}

							classesForInterface.add(clazz);

						}
					}

					// register services
					if (Service.class.isAssignableFrom(clazz)) {

						Services.getInstance().registerServiceClass(clazz);
					}

					// register agents
					if (Agent.class.isAssignableFrom(clazz)) {

						String simpleName = clazz.getSimpleName();
						String fullName   = clazz.getName();

						agentClassCache.put(simpleName, clazz);
						agentPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

					}
				}
				
			} catch (Throwable t) {}

		}

	}

	private Module loadResource(String resource) throws IOException {

		// create module
		DefaultModule ret   = new DefaultModule(resource);
		Set<String> classes = ret.getClasses();

		if (resource.endsWith(".jar") || resource.endsWith(".war")) {

			ZipFile zipFile = new ZipFile(new File(resource), ZipFile.OPEN_READ);

			// conventions that might be useful here:
			// ignore entries beginning with meta-inf/
			// handle entries beginning with images/ as IMAGE
			// handle entries beginning with pages/ as PAGES
			// handle entries ending with .jar as libraries, to be deployed to WEB-INF/lib
			// handle other entries as potential page and/or entity classes
			// .. to be extended
			// (entries that end with "/" are directories)
			for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {

				ZipEntry entry   = entries.nextElement();
				String entryName = entry.getName();

				if (entryName.endsWith(".class")) {

					String fileEntry = entry.getName().replaceAll("[/]+", ".");
					
					// add class entry to Module
					classes.add(fileEntry.substring(0, fileEntry.length() - 6));

				}

			}

			zipFile.close();

		} else if (resource.endsWith(classesDir)) {

			addClassesRecursively(new File(resource), classesDir, classes);

		} else if (resource.endsWith(testClassesDir)) {

			addClassesRecursively(new File(resource), testClassesDir, classes);
		}

		return ret;
	}

	private void addClassesRecursively(File dir, String prefix, Set<String> classes) {

		if (dir == null) {
			return;
		}
		
		int prefixLen = prefix.length();
		File[] files  = dir.listFiles();

		if (files == null) {
			return;
		}
		
		for (File file : files) {

			if (file.isDirectory()) {

				addClassesRecursively(file, prefix, classes);
				
			} else {

				try {

					String fileEntry = file.getAbsolutePath();

					fileEntry = fileEntry.substring(0, fileEntry.length() - 6);
					fileEntry = fileEntry.substring(fileEntry.indexOf(prefix) + prefixLen);
					fileEntry = fileEntry.replaceAll("[".concat(fileSepEscaped).concat("]+"), ".");
					
					if (fileEntry.startsWith(".")) {
						fileEntry = fileEntry.substring(1);
					}
					
					classes.add(fileEntry);

				} catch (Throwable t) {

					// ignore
					t.printStackTrace();
				}

			}

		}

	}

	private Relation instantiate(final Class clazz) {
		
		try {
			
			return (Relation) clazz.newInstance();
			
		} catch (Throwable t) {
			
		}
		
		return null;
	}

	private String getCombinedType(final String sourceType, final String relType, final String targetType) {
		
		return
			sourceType
			.concat(DefaultFactoryDefinition.COMBINED_RELATIONSHIP_KEY_SEP)
			.concat(relType)
			.concat(DefaultFactoryDefinition.COMBINED_RELATIONSHIP_KEY_SEP)
			.concat(targetType);
	}

	/**
	 * Scans the class path and returns a Set containing all structr modules.
	 *
	 * @return a Set of active module names
	 */
	private Set<String> getResourcesToScan() {

		String classPath	= System.getProperty("java.class.path");
		Set<String> modules	= new LinkedHashSet<>();
		Pattern pattern		= Pattern.compile(".*(structr).*(war|jar)");
		Matcher matcher		= pattern.matcher("");

		for (String jarPath : classPath.split("[".concat(pathSep).concat("]+"))) {

			String lowerPath = jarPath.toLowerCase();

			if (lowerPath.endsWith(classesDir) || lowerPath.endsWith(testClassesDir)) {

				modules.add(jarPath);
				
			} else {

				String moduleName = lowerPath.substring(lowerPath.lastIndexOf(pathSep) + 1);

				matcher.reset(moduleName);

				if (matcher.matches()) {

					modules.add(jarPath);
				}

			}

		}

		for (String resource : Services.getInstance().getResources()) {

			String lowerResource = resource.toLowerCase();

			if (lowerResource.endsWith(".jar") || lowerResource.endsWith(".war")) {

				modules.add(resource);
			}

		}

		return modules;
	}
	
	private <T> Map<Field, T> getFieldValuesOfType(Class<T> entityType, Object entity) {
		
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
	
	private Set<Class<?>> getAllTypes(Class<?> type) {

		Set<Class<?>> types = new LinkedHashSet<>();
		Class localType     = type;
			
		do {
			
			collectAllInterfaces(localType, types);
			types.add(localType);
			
			localType = localType.getSuperclass();

		} while (!localType.equals(Object.class));
		
		return types;
	}
	
	private void collectAllInterfaces(Class<?> type, Set<Class<?>> interfaces) {

		if (interfaces.contains(type)) {
			return;
		}
		
		for (Class iface : type.getInterfaces()) {
			
			collectAllInterfaces(iface, interfaces);
			interfaces.add(iface);
		}
	}

	private Map<String, Set<PropertyKey>> getPropertyViewMapForType(Class type) {

		Map<String, Set<PropertyKey>> propertyViewMap = globalPropertyViewMap.get(type);

		if (propertyViewMap == null) {

			propertyViewMap = new LinkedHashMap<>();

			globalPropertyViewMap.put(type, propertyViewMap);

		}

		return propertyViewMap;
	}

	private Map<String, PropertyKey> getClassDBNamePropertyMapForType(Class type) {

		Map<String, PropertyKey> classDBNamePropertyMap = globalClassDBNamePropertyMap.get(type);

		if (classDBNamePropertyMap == null) {

			classDBNamePropertyMap = new LinkedHashMap<>();

			globalClassDBNamePropertyMap.put(type, classDBNamePropertyMap);

		}

		return classDBNamePropertyMap;
	}

	private Map<String, PropertyKey> getClassJSNamePropertyMapForType(Class type) {

		Map<String, PropertyKey> classJSNamePropertyMap = globalClassJSNamePropertyMap.get(type);

		if (classJSNamePropertyMap == null) {

			classJSNamePropertyMap = new LinkedHashMap<>();

			globalClassJSNamePropertyMap.put(type, classJSNamePropertyMap);

		}

		return classJSNamePropertyMap;
	}

	private Map<PropertyKey, Set<PropertyValidator>> getPropertyValidatorMapForType(Class type) {

		Map<PropertyKey, Set<PropertyValidator>> validatorMap = globalValidatorMap.get(type);

		if (validatorMap == null) {

			validatorMap = new LinkedHashMap<>();

			globalValidatorMap.put(type, validatorMap);

		}

		return validatorMap;
	}

	private Map<String, PropertyGroup> getAggregatedPropertyGroupMapForType(Class type) {

		Map<String, PropertyGroup> groupMap = globalAggregatedPropertyGroupMap.get(type);

		if (groupMap == null) {

			groupMap = new LinkedHashMap<>();

			globalAggregatedPropertyGroupMap.put(type, groupMap);

		}

		return groupMap;
	}

	private Map<String, PropertyGroup> getPropertyGroupMapForType(Class type) {

		Map<String, PropertyGroup> groupMap = globalPropertyGroupMap.get(type);

		if (groupMap == null) {

			groupMap = new LinkedHashMap<>();

			globalPropertyGroupMap.put(type, groupMap);

		}

		return groupMap;
	}

	private Set<Transformation<GraphObject>> getEntityCreationTransformationsForType(Class type) {

		Set<Transformation<GraphObject>> transformations = globalTransformationMap.get(type);

		if (transformations == null) {

			transformations = new LinkedHashSet<>();

			globalTransformationMap.put(type, transformations);

		}

		return transformations;
	}
	
	private Map<String, ViewTransformation> getViewTransformationMapForType(Class type) {
		
		Map<String, ViewTransformation> viewTransformationMap = viewTransformations.get(type);
		if(viewTransformationMap == null) {
			viewTransformationMap = new LinkedHashMap<>();
			viewTransformations.put(type, viewTransformationMap);
		}
		
		return viewTransformationMap;
	}
	
	private String denormalizeEntityName(String normalizedEntityName) {
		
		StringBuilder buf = new StringBuilder();
		
		for (char c : normalizedEntityName.toCharArray()) {
			
			if (Character.isUpperCase(c) && buf.length() > 0) {
				buf.append("_");
			}
			
			buf.append(Character.toLowerCase(c));
		}
		
		return buf.toString();
	}
}
