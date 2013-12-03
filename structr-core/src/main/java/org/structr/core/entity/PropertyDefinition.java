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
package org.structr.core.entity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.lucene.search.BooleanClause.Occur;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */

public class PropertyDefinition<T> extends AbstractNode implements PropertyKey<T> {
	
	private static final Logger logger = Logger.getLogger(PropertyDefinition.class.getName());

	// public static final NodeExtender nodeExtender = new NodeExtender(GenericNode.class, "org.structr.core.entity.dynamic");
	
	public static final Property<String>               validationExpression     = new StringProperty("validationExpression");
	public static final Property<String>               validationErrorMessage   = new StringProperty("validationErrorMessage");
	public static final Property<String>               kind                     = new StringProperty("kind").indexed();
	public static final Property<String>               dataType                 = new StringProperty("dataType").indexed();
	public static final Property<String>               defaultValue             = new StringProperty("defaultValue");

	public static final Property<String>               relKind                  = new StringProperty("relKind").indexed();
	public static final Property<String>               relType                  = new StringProperty("relType").indexed();
	public static final Property<Boolean>              incoming                 = new BooleanProperty("incoming");
  
	public static final Property<Boolean>              systemProperty           = new BooleanProperty("systemProperty");
	public static final Property<Boolean>              readOnlyProperty         = new BooleanProperty("readOnlyProperty");
	public static final Property<Boolean>              writeOnceProperty        = new BooleanProperty("writeOnceProperty");
	public static final Property<Boolean>              indexedProperty          = new BooleanProperty("indexedProperty");
	public static final Property<Boolean>              passivelyIndexedProperty = new BooleanProperty("passivelyIndexedProperty");
	public static final Property<Boolean>              searchableProperty       = new BooleanProperty("searchableProperty");
	public static final Property<Boolean>              indexedWhenEmptyProperty = new BooleanProperty("indexedWhenEmptyProperty");
	
	public static final org.structr.common.View publicView = new org.structr.common.View(PropertyDefinition.class, PropertyView.Public,
		name, dataType, defaultValue, kind, relKind, relType, incoming, validationExpression, validationErrorMessage,
		systemProperty, readOnlyProperty, writeOnceProperty, indexedProperty, passivelyIndexedProperty, searchableProperty, indexedWhenEmptyProperty
	);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(PropertyDefinition.class, PropertyView.Ui,
	    name, dataType, defaultValue, kind, relKind, relType, incoming, validationExpression, validationErrorMessage, systemProperty, readOnlyProperty, writeOnceProperty, indexedProperty, passivelyIndexedProperty, searchableProperty, indexedWhenEmptyProperty
	);
	
	// ----- private members -----
	private static final Map<String, Map<String, PropertyDefinition>> dynamicTypes = new ConcurrentHashMap<>();
	private static final Map<String, Class<? extends PropertyKey>> delegateMap     = new LinkedHashMap<>();
	private List<PropertyValidator> validators                                     = new LinkedList<>();
	private Class declaringClass                                                   = null;
	private PropertyKey<T> delegate                                                = null;

	// ----- static initializer -----
	static {
		
		delegateMap.put("String",     StringProperty.class);
		delegateMap.put("Integer",    IntProperty.class);
		delegateMap.put("Long",       LongProperty.class);
		delegateMap.put("Double",     DoubleProperty.class);
		delegateMap.put("Boolean",    BooleanProperty.class);
		delegateMap.put("Date",       ISO8601DateProperty.class);
//		delegateMap.put("Collection", EndNodes.class);
//		delegateMap.put("Entity",     End.class);
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
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		clearPropertyDefinitions();
		return true;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		clearPropertyDefinitions();
		return true;
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap removedProperties) throws FrameworkException{

		clearPropertyDefinitions();
		return true;
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
	public void jsonName(String jsonName) {
	}

	@Override
	public void dbName(String dbName) {
	}

	@Override
	public String typeName() {
		
		if (delegate != null) {
			return delegate.typeName();
		}
		
		return null;
	}

	@Override
	public Integer getSortType() {
		// TODO: make sorting of dynamic properties possible!
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
	public T defaultValue() {
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
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occur occur, T searchValue, boolean exactMatch) {
		
		if (delegate != null) {
			return delegate.getSearchAttribute(securityContext, occur, searchValue, exactMatch);
		}
		
		return null;
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		if (delegate != null) {
			return delegate.getProperty(securityContext, obj, applyConverter);
		}
		
		return null;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		
		if (delegate != null) {
			delegate.setProperty(securityContext, obj, value);
		}
	}

	@Override
	public void registrationCallback(Class entityType) {
	}

	@Override
	public boolean isUnvalidated() {
		return getProperty(PropertyDefinition.systemProperty);
	}

	@Override
	public boolean isReadOnly() {
		return getProperty(PropertyDefinition.readOnlyProperty);
	}

	@Override
	public boolean isWriteOnce() {
		return getProperty(PropertyDefinition.writeOnceProperty);
	}

	@Override
	public boolean isIndexed() {
		return getProperty(PropertyDefinition.indexedProperty);
	}

	@Override
	public boolean isPassivelyIndexed() {
		return getProperty(PropertyDefinition.passivelyIndexedProperty);
	}

	@Override
	public boolean isSearchable() {
		return getProperty(PropertyDefinition.searchableProperty);
	}

	@Override
	public boolean isIndexedWhenEmpty() {
		return getProperty(PropertyDefinition.indexedWhenEmptyProperty);
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
	
		/*
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

					if (EndNodes.class.isAssignableFrom(keyClass) || End.class.isAssignableFrom(keyClass)) {
					
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
							// StructrApp.getConfiguration().registerProperty(dataClass, delegate);

						} catch (Throwable t) {

							t.printStackTrace();
						}

					} else {

						try {

							// if this call fails, a convention has been violated
							delegate = keyClass.getConstructor(String.class).newInstance(_name);

							// register property
							// StructrApp.getConfiguration().registerProperty(dataClass, delegate);

						} catch (Throwable t) {

							t.printStackTrace();
						}
					}
				}
				
				delegate.setDeclaringClass(dataClass);
			}
		}
		*/
	}

	// ----- static methods -----
	private static void update() {
		
		if (dynamicTypes.isEmpty()) {
			
			try {
				final List<PropertyDefinition> propertyDefinitions = StructrApp.getInstance().nodeQuery(PropertyDefinition.class).getAsList();
				for (PropertyDefinition def : propertyDefinitions) {
					
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
			
			definitionsForKind = new ConcurrentHashMap<>();
			dynamicTypes.put(kind, definitionsForKind);
		}
		
		return definitionsForKind;
	}

	@Override
	public void addValidator(PropertyValidator validator) {
		validators.add(validator);
	}

	@Override
	public List getValidators() {
		return validators;
	}
	
	@Override
	public boolean requiresSynchronization() {
		return false;
	}
	
	@Override
	public String getSynchronizationKey() {
		return null;
	}

	@Override
	public void index(GraphObject entity, Object value) {
		
		if (delegate != null) {
			delegate.index(entity, value);
		}
	}

	@Override
	public List extractSearchableAttribute(SecurityContext securityContext, HttpServletRequest request, boolean looseSearch) throws FrameworkException {
		
		if (delegate != null) {
			return delegate.extractSearchableAttribute(securityContext, request, looseSearch);
		}
		
		return Collections.emptyList();
	}

	@Override
	public T extractSearchableAttribute(SecurityContext securityContext, String requestParameter) throws FrameworkException {
		
		if (delegate != null) {
			return delegate.extractSearchableAttribute(securityContext, requestParameter);
		}
		
		return null;
	}

	@Override
	public Property<T> indexed() {
		
		if (delegate != null) {
			return delegate.indexed();
		}
		
		return null;
	}

	@Override
	public Property<T> indexed(NodeService.NodeIndex nodeIndex) {
		
		if (delegate != null) {
			return delegate.indexed(nodeIndex);
		}
		
		return null;
	}

	@Override
	public Property<T> indexed(NodeService.RelationshipIndex relIndex) {
		
		if (delegate != null) {
			return delegate.indexed(relIndex);
		}
		
		return null;
	}

	@Override
	public Property<T> passivelyIndexed() {
		
		if (delegate != null) {
			return delegate.passivelyIndexed();
		}
		
		return null;
	}

	@Override
	public Property<T> passivelyIndexed(NodeService.NodeIndex nodeIndex) {
		
		if (delegate != null) {
			return delegate.passivelyIndexed(nodeIndex);
		}
		
		return null;
	}

	@Override
	public Property<T> passivelyIndexed(NodeService.RelationshipIndex relIndex) {
		
		if (delegate != null) {
			return delegate.passivelyIndexed(relIndex);
		}
		
		return null;
	}

	@Override
	public Property<T> indexedWhenEmpty() {
		
		if (delegate != null) {
			return delegate.indexedWhenEmpty();
		}
		
		return null;
	}
}
