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
package org.structr.schema;

import org.structr.agent.Agent;
import org.structr.api.service.LicenseManager;
import org.structr.common.FactoryDefinition;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;
import org.structr.core.PropertyValidator;
import org.structr.core.Transformation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.module.StructrModule;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public interface ConfigurationProvider {

	public void initialize(final LicenseManager licenseManager);
	public void shutdown();

	public void unregisterEntityType(final Class oldType);
	public void registerEntityType(final Class newType);
	public void registerEntityCreationTransformation(final Class type, final Transformation<GraphObject> transformation);

	public Map<String, Class<? extends Agent>> getAgents();
	public Map<String, Class<? extends NodeInterface>> getNodeEntities();
	public Map<String, Class<? extends RelationshipInterface>> getRelationshipEntities();
	public Map<String, Class> getInterfaces();

	public Set<Class> getClassesForInterface(final String simpleName);

	public void registerPropertyGroup(final Class entityClass, final PropertyKey propertyKey, final PropertyGroup propertyGroup);
	public void registerConvertedProperty(final PropertyKey property);

	public Class getNodeEntityClass(final String name);
	public Class getRelationshipEntityClass(final String name);

	public void setRelationClassForCombinedType(final String combinedType, final Class clazz);
	public void setRelationClassForCombinedType(final String sourceType, final String relType, final String targetType, final Class clazz);
	public Class getRelationClassForCombinedType(final String sourceType, final String relType, final String targetType);

	public Set<Transformation<GraphObject>> getEntityCreationTransformations(final Class type);

	public PropertyGroup getPropertyGroup(final Class type, final PropertyKey key);

	public PropertyGroup getPropertyGroup(final Class type, final String key);

	public Set<String> getPropertyViews();
	public Set<String> getPropertyViewsForType(final Class type);
	public void registerDynamicViews(final Set<String> dynamicViews);
	public boolean hasView(final Class type, final String propertyView);

	public void registerPropertySet(final Class type, final String propertyView, final PropertyKey... propertyKey);
	public void registerPropertySet(final Class type, final String propertyView, final String propertyName);
	public Set<PropertyKey> getPropertySet(final Class type, final String propertyView);

	public PropertyKey getPropertyKeyForDatabaseName(final Class type, final String dbName);
	public PropertyKey getPropertyKeyForDatabaseName(final Class type, final String dbName, final boolean createGeneric);

	public PropertyKey getPropertyKeyForJSONName(final Class type, final String jsonName);
	public PropertyKey getPropertyKeyForJSONName(final Class type, final String jsonName, final boolean createIfNotFound);

	public void setPropertyKeyForJSONName(final Class type, final String jsonName, final PropertyKey key);

	public Set<PropertyValidator> getPropertyValidators(final SecurityContext securityContext, final Class type, final PropertyKey propertyKey);

	public Set<Class> getInterfacesForType(final Class type);

	public Map<String, Method> getExportedMethodsForType(final Class type);

	public boolean isKnownProperty(final PropertyKey key);

	public FactoryDefinition getFactoryDefinition();

	public void registerFactoryDefinition(final FactoryDefinition factory);

	public Map<String, Method> getAnnotatedMethods(final Class entityType, final Class annotationType);

	/**
	 * Registers the given property with the given type.
	 *
	 * @param type
	 * @param propertyKey
	 */
	public void registerProperty(final Class type, final PropertyKey propertyKey);

	/**
	 * Unregisters the given property with the given type.
	 *
	 * @param type
	 * @param propertyKey
	 */
	public void unregisterProperty(final Class type, final PropertyKey propertyKey);

	/**
	 * Registers the given property with the given type AND ALL SUPERTYPES.
	 *
	 * @param type
	 * @param propertyKey
	 */
	public void registerDynamicProperty(final Class type, final PropertyKey propertyKey);

	Map<String, StructrModule> getModules();
	Map<String, Map<String, PropertyKey>> getTypeAndPropertyMapping();
}
