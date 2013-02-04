package org.structr.web.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchOperator;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.AbstractRelationProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.EntityNotionProperty;
import org.structr.core.property.EntityProperty;
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

	public static final Property<String>               validationExpression   = new StringProperty("validationExpression");
	public static final Property<String>               validationErrorMessage = new StringProperty("validationErrorMessage");
	public static final Property<String>               dataType               = new StringProperty("dataType");
	public static final Property<String>               relType                = new StringProperty("relType");
	
	public static final Property<Boolean>              systemProperty         = new BooleanProperty("systemProperty");
	public static final Property<Boolean>              readOnlyProperty       = new BooleanProperty("readOnlyProperty");
	public static final Property<Boolean>              writeOnceProperty      = new BooleanProperty("writeOnceProperty");
	
	public static final EntityProperty<Type>           typeNode               = new EntityProperty<Type>("typeNode", Type.class, RelType.DEFINES_PROPERTY, Direction.INCOMING, true);
	public static final Property<String>               kind                   = new EntityNotionProperty("kind", typeNode, new PropertyNotion(Type.kind));
	public static final Property<String>               typeId                 = new EntityIdProperty("typeId", typeNode);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(Type.class, PropertyView.Public,
	    name, dataType, kind, relType, validationExpression, validationErrorMessage, systemProperty, readOnlyProperty, writeOnceProperty
	);
	
	// ----- private members -----
	private static final Map<String, Class<? extends PropertyKey>> delegateMap = new LinkedHashMap<String, Class<? extends PropertyKey>>();
	private Class declaringClass                                               = null;
	private PropertyKey delegate                                               = null;

	// ----- static initializer -----
	static {
		
		delegateMap.put("String",     StringProperty.class);
		delegateMap.put("Integer",    IntProperty.class);
		delegateMap.put("Long",       LongProperty.class);
		delegateMap.put("Double",     DoubleProperty.class);
		delegateMap.put("Boolean",    BooleanProperty.class);
		delegateMap.put("Collection", CollectionProperty.class);
		delegateMap.put("Entity",     EntityProperty.class);
		
	}
	
	// ----- overridden methods from superclass -----
	@Override
	public void onNodeInstantiation() {
		initialize();
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
	public String getSearchStringValue(Object source) {
		
		if (delegate != null) {
			return delegate.getSearchStringValue(source);
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
	
	// ----- overridden methods from superclass -----
	@Override
	public void afterDeletion(SecurityContext securityContext) {
		
	}
	
	// ----- private methods -----
	private void initialize() {
		
		if (delegate == null) {
			
			String _dataType = super.getProperty(PropertyDefinition.dataType);
			String _relType  = super.getProperty(PropertyDefinition.relType);
			String _name     = super.getName();

			if (_dataType != null && _name != null) {

				Class<? extends PropertyKey> keyClass = delegateMap.get(_dataType);
				if (keyClass != null) {

					if (AbstractRelationProperty.class.isAssignableFrom(keyClass)) {
					
						try {

							// if this call fails, a convention has been violated
							delegate = keyClass.getConstructor(String.class, Class.class, RelationshipType.class, Boolean.TYPE).newInstance(
							                   _name,
									   DataNode.class,
									   DynamicRelationshipType.withName(_relType),
									   false
							           );

							// register property
							EntityContext.registerProperty(DataNode.class, delegate);

						} catch (Throwable t) {

							t.printStackTrace();
						}

					} else {

						try {

							// if this call fails, a convention has been violated
							delegate = keyClass.getConstructor(String.class).newInstance(_name);

							// register property
							EntityContext.registerProperty(DataNode.class, delegate);

						} catch (Throwable t) {

							t.printStackTrace();
						}
					}
				}
				
				delegate.setDeclaringClass(DataNode.class);
			}
		}
	}
}
