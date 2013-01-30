package org.structr.web.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchOperator;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.EntityIdProperty;
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
	
	public static final Property<Boolean>              systemProperty         = new BooleanProperty("systemProperty");
	public static final Property<Boolean>              readOnlyProperty       = new BooleanProperty("readOnlyProperty");
	public static final Property<Boolean>              writeOnceProperty      = new BooleanProperty("writeOnceProperty");
	
	public static final EntityProperty<TypeDefinition> typeDefinition         = new EntityProperty<TypeDefinition>("typeDefinition", TypeDefinition.class, RelType.DEFINES_PROPERTY, Direction.INCOMING, true);
	public static final Property<String>               typeDefinitionId       = new EntityIdProperty("typeDefinitionId", typeDefinition);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(TypeDefinition.class, PropertyView.Public,
	    name, dataType, validationExpression, validationErrorMessage, systemProperty, readOnlyProperty, writeOnceProperty
	);
	
	// ----- private members -----
	private static final Map<String, Class<? extends PropertyKey>> delegateMap = new LinkedHashMap<String, Class<? extends PropertyKey>>();
	private Class declaringClass                                               = null;
	private PropertyKey delegate                                               = null;

	// ----- static initializer -----
	static {
		
		delegateMap.put("String",  StringProperty.class);
		delegateMap.put("Integer", IntProperty.class);
		delegateMap.put("Long",    LongProperty.class);
		delegateMap.put("Double",  DoubleProperty.class);
		delegateMap.put("Boolean", BooleanProperty.class);
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
		
		getPropertyKeyForDataType();
		
		if (delegate != null) {
			return delegate.typeName();
		}
		
		return null;
	}

	@Override
	public Class relatedType() {
		
		getPropertyKeyForDataType();
		
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
		
		getPropertyKeyForDataType();
		
		if (delegate != null) {
			return delegate.databaseConverter(securityContext, entity);
		}
		
		return null;
	}

	@Override
	public PropertyConverter inputConverter(SecurityContext securityContext) {
		
		getPropertyKeyForDataType();
		
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
		
		getPropertyKeyForDataType();
		
		if (delegate != null) {
			return delegate.getSearchAttribute(op, searchValue, exactMatch);
		}
		
		return null;
	}

	@Override
	public void registerSearchableProperties(Set searchableProperties) {
		
		getPropertyKeyForDataType();
		
		if (delegate != null) {
			delegate.registerSearchableProperties(searchableProperties);
		}
	}

	@Override
	public String getSearchStringValue(Object source) {
		
		getPropertyKeyForDataType();
		
		if (delegate != null) {
			return delegate.getSearchStringValue(source);
		}
		
		return null;
	}

	@Override
	public Object getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		getPropertyKeyForDataType();
		
		if (delegate != null) {
			return delegate.getProperty(securityContext, obj, applyConverter);
		}
		
		return null;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, Object value) throws FrameworkException {
		
		getPropertyKeyForDataType();
		
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
		
		getPropertyKeyForDataType();
		
		if (delegate != null) {
			return delegate.isCollection();
		}
		
		return false;
	}
	
	// ----- private methods -----
	private void getPropertyKeyForDataType() {
		
		if (delegate == null) {
			
			String _dataType = super.getProperty(PropertyDefinition.dataType);
			String _name     = super.getName();

			if (_dataType != null && _name != null) {

				Class<? extends PropertyKey> keyClass = delegateMap.get(_dataType);
				if (keyClass != null) {

					try {

						// if this call fails, a convention has been violated
						delegate = keyClass.getConstructor(String.class).newInstance(_name);

					} catch (Throwable t) {

						t.printStackTrace();
					}
				}
			}
		}
	}
}
