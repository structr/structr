package org.structr.schema;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import org.structr.common.FactoryDefinition;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;
import org.structr.core.PropertyValidator;
import org.structr.core.Transformation;
import org.structr.core.ViewTransformation;
import org.structr.agent.Agent;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public interface ConfigurationProvider {

	public void initialize();
	public void shutdown();

	public void unregisterEntityType(final String typeName);
	public void registerEntityType(final Class newType);
	public void registerEntityCreationTransformation(final Class type, final Transformation<GraphObject> transformation);
	
	public Map<String, Class<? extends Agent>> getAgents();
	public Map<String, Class<? extends NodeInterface>> getNodeEntities();
	public Map<String, Class<? extends RelationshipInterface>> getRelationshipEntities();
	public Map<String, Class> getInterfaces();
	
	public Set<Class> getClassesForInterface(final String simpleName);
	
	
	// sort me
	

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
	
	public void registerViewTransformation(final Class type, final String view, final ViewTransformation transformation);
	
	public ViewTransformation getViewTransformation(final Class type, final String view);
	
	public Set<String> getPropertyViews();
	public void registerDynamicViews(final Set<String> dynamicViews);
	
	public void registerPropertySet(final Class type, final String propertyView, final PropertyKey... propertyKey);
	public Set<PropertyKey> getPropertySet(final Class type, final String propertyView);
	
	public PropertyKey getPropertyKeyForDatabaseName(final Class type, final String dbName);
	
	public PropertyKey getPropertyKeyForDatabaseName(final Class type, final String dbName, final boolean createGeneric);
	
	public PropertyKey getPropertyKeyForJSONName(final Class type, final String jsonName);
	
	public PropertyKey getPropertyKeyForJSONName(final Class type, final String jsonName, final boolean createIfNotFound);
	
	public Set<PropertyValidator> getPropertyValidators(final SecurityContext securityContext, final Class type, final PropertyKey propertyKey);
	
	public Set<Class> getInterfacesForType(final Class type);
	
	public Set<Method> getExportedMethodsForType(final Class type);
	
	public boolean isKnownProperty(final PropertyKey key);
	
	public FactoryDefinition getFactoryDefinition();
	
	public void registerFactoryDefinition(final FactoryDefinition factory);
	
	public Set<Method> getAnnotatedMethods(final Class entityType, final Class annotationType);

}
