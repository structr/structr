package org.structr.core.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.experimental.NodeExtender;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchOperator;
import org.structr.core.notion.Notion;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.AbstractRelationProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */

public class PropertyDefinition extends AbstractNode implements PropertyKey {
	
	private static final Logger logger = Logger.getLogger(PropertyDefinition.class.getName());

	public static final NodeExtender nodeExtender = new NodeExtender(GenericNode.class, "org.structr.core.entity.dynamic");
	
	public static final Property<String>               validationExpression   = new StringProperty("validationExpression");
	public static final Property<String>               validationErrorMessage = new StringProperty("validationErrorMessage");
	public static final Property<String>               kind                   = new StringProperty("kind");
	public static final Property<String>               dataType               = new StringProperty("dataType");
	public static final Property<String>               relKind                = new StringProperty("relKind");
	public static final Property<String>               relType                = new StringProperty("relType");
	public static final Property<Boolean>              incoming               = new BooleanProperty("incoming");

	public static final Property<Boolean>              systemProperty         = new BooleanProperty("systemProperty");
	public static final Property<Boolean>              readOnlyProperty       = new BooleanProperty("readOnlyProperty");
	public static final Property<Boolean>              writeOnceProperty      = new BooleanProperty("writeOnceProperty");
	
	public static final org.structr.common.View publicView = new org.structr.common.View(PropertyDefinition.class, PropertyView.Public,
	    name, dataType, kind, relKind, relType, incoming, validationExpression, validationErrorMessage, systemProperty, readOnlyProperty, writeOnceProperty
	);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(PropertyDefinition.class, PropertyView.Ui,
	    name, dataType, kind, relKind, relType, incoming, validationExpression, validationErrorMessage, systemProperty, readOnlyProperty, writeOnceProperty
	);
	
	// ----- private members -----
	private static final Map<String, Map<String, PropertyDefinition>> dynamicTypes = new ConcurrentHashMap<String, Map<String, PropertyDefinition>>();
	private static final Map<String, Class<? extends PropertyKey>> delegateMap     = new LinkedHashMap<String, Class<? extends PropertyKey>>();
	private Class declaringClass                                                   = null;
	private PropertyKey delegate                                                   = null;

	// ----- static initializer -----
	static {
		
		delegateMap.put("String",     StringProperty.class);
		delegateMap.put("Integer",    IntProperty.class);
		delegateMap.put("Long",       LongProperty.class);
		delegateMap.put("Double",     DoubleProperty.class);
		delegateMap.put("Boolean",    BooleanProperty.class);
		delegateMap.put("Date",       ISO8601DateProperty.class);
		delegateMap.put("Collection", CollectionProperty.class);
		delegateMap.put("Entity",     EntityProperty.class);
		
		EntityContext.registerSearchablePropertySet(PropertyDefinition.class, NodeIndex.keyword.name(), dataType, kind, relKind, relType);
		EntityContext.registerSearchablePropertySet(PropertyDefinition.class, NodeIndex.fulltext.name(), dataType, kind, relKind, relType);
	}
	
	public static void clearPropertyDefinitions() {
		
		for (Map<String, PropertyDefinition> kinds : dynamicTypes.values()) {
			kinds.clear();
		}
			
		dynamicTypes.clear();
	}
	
	public static boolean exists(String kind) {
		
		if (kind != null) {
			
			update();
			
			return dynamicTypes.containsKey(kind);
		}
		
		return false;
	}
	
	public static Iterable<PropertyDefinition> getPropertiesForKind(String kind) {
		
		if (kind != null) {
			
			update();
			
			Map<String, PropertyDefinition> definitions = dynamicTypes.get(kind);
			if (definitions != null) {
				return definitions.values();
			}
		}
		
		return null;
	}
	
	public static PropertyDefinition getPropertyForKind(String kind, PropertyKey key) {
		
		if (kind != null) {
			
			update();
			
			Map<String, PropertyDefinition> definitions = dynamicTypes.get(kind);
			if (definitions != null) {
				return definitions.get(key.dbName());
			}
		}
		
		return null;
	}

	// ----- overriden methods from superclass -----
	@Override
	public void onNodeInstantiation() {
		initialize();
	}
	
	@Override
	public void afterCreation(SecurityContext securityContext) {
		clearPropertyDefinitions();
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
		clearPropertyDefinitions();
	}

	@Override
	public void afterDeletion(SecurityContext securityContext) {
		clearPropertyDefinitions();
	}
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		
		boolean error = false;
		
		error |= ValidationHelper.checkStringNotBlank(this, name,     errorBuffer);
		error |= ValidationHelper.checkStringNotBlank(this, kind,     errorBuffer);
		error |= ValidationHelper.checkStringNotBlank(this, dataType, errorBuffer);
		
		String _dataType = getProperty(PropertyDefinition.dataType);
		if (_dataType != null) {
			
			if ("Entity".equals(_dataType) || "Collection".equals(_dataType)) {
				
				error |= ValidationHelper.checkStringNotBlank(this, relKind, errorBuffer);
				error |= ValidationHelper.checkStringNotBlank(this, relType, errorBuffer);
			}
		}
		
		error |= !super.isValid(errorBuffer);
		
		return !error;
	}
	
	// ----- interface PropertyKey -----
	@Override
	public String jsonName() {
		return getProperty(AbstractNode.name);
	}

	@Override
	public String dbName() {
		return getProperty(AbstractNode.name);
	}

	@Override
	public String typeName() {
		
		if (delegate != null) {
			return delegate.typeName();
		}
		
		return null;
	}

	@Override
	public Class relatedType() {
		
		if (delegate != null) {
			return delegate.relatedType();
		}
		
		return null;
	}

	@Override
	public Object defaultValue() {
		return null;
	}
	
	@Override
	public PropertyConverter databaseConverter(SecurityContext securityContext) {

		return databaseConverter(securityContext, null);

	}

	@Override
	public PropertyConverter databaseConverter(SecurityContext securityContext, GraphObject entity) {
		
		if (delegate != null) {
			return delegate.databaseConverter(securityContext, entity);
		}
		
		return null;
	}

	@Override
	public PropertyConverter inputConverter(SecurityContext securityContext) {
		
		if (delegate != null) {
			return delegate.inputConverter(securityContext);
		}
		
		return null;
	}

	@Override
	public void setDeclaringClass(Class declaringClass) {
		this.declaringClass = declaringClass;
	}

	@Override
	public Class getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public SearchAttribute getSearchAttribute(SearchOperator op, Object searchValue, boolean exactMatch) {
		
		if (delegate != null) {
			return delegate.getSearchAttribute(op, searchValue, exactMatch);
		}
		
		return null;
	}

	@Override
	public void registerSearchableProperties(Set searchableProperties) {
		
		if (delegate != null) {
			delegate.registerSearchableProperties(searchableProperties);
		}
	}

	@Override
	public Object getSearchValue(Object source) {
		
		if (delegate != null) {
			return delegate.getSearchValue(source);
		}
		
		return null;
	}

	@Override
	public Object getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		if (delegate != null) {
			return delegate.getProperty(securityContext, obj, applyConverter);
		}
		
		return null;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, Object value) throws FrameworkException {
		
		if (delegate != null) {
			delegate.setProperty(securityContext, obj, value);
		}
	}

	@Override
	public void registrationCallback(Class entityType) {
	}

	@Override
	public boolean isSystemProperty() {
		return getProperty(PropertyDefinition.systemProperty);
	}

	@Override
	public boolean isReadOnlyProperty() {
		return getProperty(PropertyDefinition.readOnlyProperty);
	}

	@Override
	public boolean isWriteOnceProperty() {
		return getProperty(PropertyDefinition.writeOnceProperty);
	}

	@Override
	public boolean isCollection() {
		
		if (delegate != null) {
			return delegate.isCollection();
		}
		
		return false;
	}
	
	// ----- private methods -----
	private void initialize() {
		
		if (delegate == null) {
			
			String _kind        = super.getProperty(PropertyDefinition.kind);
			String _dataType    = super.getProperty(PropertyDefinition.dataType);
			String _relType     = super.getProperty(PropertyDefinition.relType);
			String _relKind     = super.getProperty(PropertyDefinition.relKind);
			Direction direction = super.getProperty(PropertyDefinition.incoming) ? Direction.INCOMING : Direction.OUTGOING;
			String _name        = super.getName();

			if (_dataType != null && _name != null) {

				Class<? extends PropertyKey> keyClass  = delegateMap.get(_dataType);
				Class<? extends GenericNode> dataClass = nodeExtender.getType(_kind);

				if (dataClass == null) {
					dataClass = GenericNode.class;
				}
				
				if (keyClass != null) {

					if (AbstractRelationProperty.class.isAssignableFrom(keyClass)) {
					
						try {

							Class _relClass = nodeExtender.getType(_relKind);
							if (_relClass == null) {
								_relClass = GenericNode.class;
							}
							
							// if this call fails, a convention has been violated
							delegate = keyClass.getConstructor(String.class, Class.class, RelationshipType.class, Direction.class, Notion.class, Boolean.TYPE).newInstance(
							                   _name,
									   _relClass,
									   DynamicRelationshipType.withName(_relType),
									   direction,
									   new PropertySetNotion(GraphObject.uuid),
									   false
							           );

							// register property
							EntityContext.registerProperty(dataClass, delegate);

						} catch (Throwable t) {

							t.printStackTrace();
						}

					} else {

						try {

							// if this call fails, a convention has been violated
							delegate = keyClass.getConstructor(String.class).newInstance(_name);

							// register property
							EntityContext.registerProperty(dataClass, delegate);

						} catch (Throwable t) {

							t.printStackTrace();
						}
					}
				}
				
				delegate.setDeclaringClass(dataClass);
			}
		}
	}

	// ----- static methods -----
	private static void update() {
		
		if (dynamicTypes.isEmpty()) {
			
			try {
				SecurityContext securityContext = SecurityContext.getSuperUserInstance();

				Result<PropertyDefinition> propertyDefinitions = Services.command(securityContext, SearchNodeCommand.class).execute(
					Search.andExactType(PropertyDefinition.class.getSimpleName())
				);

				for (PropertyDefinition def : propertyDefinitions.getResults()) {
					getPropertyDefinitionsForKind(def.getProperty(PropertyDefinition.kind)).put(def.dbName(), def);
				}

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to update dynamic property types: {0}", t.getMessage());
			}
		}
	}
	
	private static Map<String, PropertyDefinition> getPropertyDefinitionsForKind(String kind) {
		
		Map<String, PropertyDefinition> definitionsForKind = dynamicTypes.get(kind);
		if (definitionsForKind == null) {
			
			definitionsForKind = new ConcurrentHashMap<String, PropertyDefinition>();
			dynamicTypes.put(kind, definitionsForKind);
		}
		
		return definitionsForKind;
	}

}
