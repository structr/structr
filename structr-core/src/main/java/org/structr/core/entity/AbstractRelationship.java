/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.entity;

import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.GetNodeByIdCommand;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.DateUtils;

import org.neo4j.graphdb.*;

import org.structr.common.*;
import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.UuidCreationTransformation;
import org.structr.common.error.*;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.common.error.NullPropertyToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.PropertyGroup;
import org.structr.core.Services;
import org.structr.core.graph.NodeService.RelationshipIndex;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;
import org.structr.core.validator.SimpleRegexValidator;

//~--- JDK imports ------------------------------------------------------------

import java.text.ParseException;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public abstract class AbstractRelationship implements GraphObject, Comparable<AbstractRelationship> {

	private static final Logger logger = Logger.getLogger(AbstractRelationship.class.getName());

	public static final Property<String>   combinedType  = new StringProperty("combinedType");
	public static final Property<Integer>  cascadeDelete = new IntProperty("cascadeDelete");
	public static final Property<Date>     createdDate   = new ISO8601DateProperty("createdDate").systemProperty();
	public static final Property<String[]> allowed       = new GenericProperty<String[]>("allowed");
	
	
	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerSearchableProperty(AbstractRelationship.class, RelationshipIndex.rel_uuid.name(), uuid);

		// register transformation for automatic uuid creation
		EntityContext.registerEntityCreationTransformation(AbstractRelationship.class, new UuidCreationTransformation());

		// register uuid validator
		EntityContext.registerPropertyValidator(AbstractRelationship.class, uuid, new SimpleRegexValidator("[a-zA-Z0-9]{32}"));
		
	}

	//~--- fields ---------------------------------------------------------

	protected Class entityType     = getClass();
	private String cachedEndNodeId = null;

	// test
	protected PropertyMap cachedConvertedProperties = new PropertyMap();
	protected PropertyMap cachedRawProperties       = new PropertyMap();
	private String cachedStartNodeId                = null;
	protected SecurityContext securityContext       = null;
	private boolean readOnlyPropertiesUnlocked      = false;

	// reference to database relationship
	protected Relationship dbRelationship;
	protected PropertyMap properties;
	protected String cachedUuid = null;
	protected boolean isDirty;

	//~--- constant enums -------------------------------------------------

	//~--- constructors ---------------------------------------------------

//      public enum Permission implements PropertyKey {
//              allowed, denied, scanEntity, showTree, write, execute, createNode, deleteNode, editProperties, addRelationship, removeRelationship, accessControl;
//      }
	public AbstractRelationship() {

		this.properties = new PropertyMap();
		isDirty         = true;

	}

	public AbstractRelationship(final PropertyMap properties) {

		this.properties = properties;
		isDirty         = true;

	}

	public AbstractRelationship(final SecurityContext securityContext, final PropertyMap data) {

		if (data != null) {

			this.securityContext = securityContext;
			this.properties      = data;
			this.isDirty         = true;

		}

	}

	public AbstractRelationship(final SecurityContext securityContext, final Relationship dbRel) {

		init(securityContext, dbRel);

	}

	//~--- methods --------------------------------------------------------

	/**
	 * Called when a relationship of this combinedType is instatiated. Please note that
	 * a relationship can (and will) be instantiated several times during a
	 * normal rendering turn.
	 */
	public void onRelationshipInstantiation() {

		try {

			if (dbRelationship != null) {

				Node startNode = dbRelationship.getStartNode();
				Node endNode   = dbRelationship.getEndNode();

				if ((startNode != null) && (endNode != null) && startNode.hasProperty(AbstractNode.uuid.dbName()) && endNode.hasProperty(AbstractNode.uuid.dbName())) {

					cachedStartNodeId = (String) startNode.getProperty(AbstractNode.uuid.dbName());
					cachedEndNodeId   = (String) endNode.getProperty(AbstractNode.uuid.dbName());

				}

			}

		} catch (Throwable t) {
		}

	}

	public AbstractNode identifyStartNode(RelationshipMapping namedRelation, Map<String, Object> propertySet) throws FrameworkException {

		Notion startNodeNotion = getStartNodeNotion();    // new RelationshipNotion(getStartNodeIdKey());

		startNodeNotion.setType(namedRelation.getSourceType());

		PropertyKey startNodeIdentifier = startNodeNotion.getPrimaryPropertyKey();

		if (startNodeIdentifier != null) {

			Object identifierValue = propertySet.get(startNodeIdentifier.jsonName());

			propertySet.remove(startNodeIdentifier.jsonName());

			return (AbstractNode) startNodeNotion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;

	}

	public AbstractNode identifyEndNode(RelationshipMapping namedRelation, Map<String, Object> propertySet) throws FrameworkException {

		Notion endNodeNotion = getEndNodeNotion();    // new RelationshipNotion(getEndNodeIdKey());

		endNodeNotion.setType(namedRelation.getDestType());

		PropertyKey endNodeIdentifier = endNodeNotion.getPrimaryPropertyKey();

		if (endNodeIdentifier != null) {

			Object identifierValue = propertySet.get(endNodeIdentifier.jsonName());

			propertySet.remove(endNodeIdentifier.jsonName());

			return (AbstractNode) endNodeNotion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;

	}

	public void init(final SecurityContext securityContext, final Relationship dbRel) {

		this.dbRelationship  = dbRel;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	public void init(final SecurityContext securityContext) {

		this.securityContext = securityContext;
		this.isDirty         = false;

	}

	public void init(final SecurityContext securityContext, final AbstractRelationship rel) {

		this.dbRelationship  = rel.dbRelationship;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {

		this.readOnlyPropertiesUnlocked = true;

	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				try {
					
					dbRelationship.removeProperty(key.dbName());

				} finally {}

				return null;

			}

		});
	}

	@Override
	public boolean equals(final Object o) {

		return (o != null && new Integer(this.hashCode()).equals(new Integer(o.hashCode())));

	}

	@Override
	public int hashCode() {

		if (this.dbRelationship == null) {

			return (super.hashCode());
		}

		return Long.valueOf(dbRelationship.getId()).hashCode();

	}

	@Override
	public int compareTo(final AbstractRelationship rel) {

		// TODO: implement finer compare methods, e.g. taking title and position into account
		if (rel == null) {

			return -1;
		}

		return ((Long) this.getId()).compareTo((Long) rel.getId());
	}

	public int cascadeDelete() {

		Integer cd = getIntProperty(AbstractRelationship.cascadeDelete);

		return (cd != null)
		       ? cd
		       : 0;

	}

	public void addPermission(final Permission permission) {

		String[] allowed = getPermissions();

		if (ArrayUtils.contains(allowed, permission.name())) {

			return;
		}

		setAllowed((String[]) ArrayUtils.add(allowed, permission.name()));

	}

	public void removePermission(final Permission permission) {

		String[] allowed = getPermissions();

		if (!ArrayUtils.contains(allowed, permission.name())) {

			return;
		}

		setAllowed((String[]) ArrayUtils.removeElement(allowed, permission.name()));

	}
	
	/**
	 * Indicates whether this relationship type propagates modifications
	 * in the given direction. Overwrite this method and return true for
	 * the desired direction to enable a callback on non-local node
	 * modification.
	 * 
	 * @param direction the direction for which the propagation should is to be returned
	 * @return the propagation status for the given direction
	 */
	public boolean propagatesModifications(Direction direction) {
		return false;
	}
	
	//~--- get methods ----------------------------------------------------

	@Override
	public PropertyKey getDefaultSortKey() {

		return null;

	}

	@Override
	public String getDefaultSortOrder() {

		return GraphObjectComparator.ASCENDING;

	}

	public abstract PropertyKey getStartNodeIdKey();

	public abstract PropertyKey getEndNodeIdKey();

	public Notion getEndNodeNotion() {

		return new RelationshipNotion(getEndNodeIdKey());

	}

	public Notion getStartNodeNotion() {

		return new RelationshipNotion(getStartNodeIdKey());

	}

	@Override
	public long getId() {

		return getInternalId();

	}
	
	@Override
	public String getUuid() {

		return getProperty(AbstractRelationship.uuid);

	}
	
	public long getRelationshipId() {

		return getInternalId();

	}

	public long getInternalId() {

		return dbRelationship.getId();

	}

	public PropertyMap getProperties() throws FrameworkException {

		Map<String, Object> properties = new LinkedHashMap<String, Object>();

		for (String key : dbRelationship.getPropertyKeys()) {

			properties.put(key, dbRelationship.getProperty(key));
		}

		// convert the database properties back to their java types
		return PropertyMap.databaseTypeToJavaType(securityContext, this, properties);

	}

	@Override
	public <T> T getProperty(final PropertyKey<T> key) {
		return getProperty(key, true);
	}

	private <T> T getProperty(final PropertyKey<T> key, boolean applyConverter) {

		// early null check, this should not happen...
		if (key == null || key.dbName() == null) {
			return null;
		}
		
		Object value      = applyConverter ? cachedConvertedProperties.get(key) : cachedRawProperties.get(key);
		boolean dontCache = false;
		Class type         = this.getClass();
		
		if(value == null || !applyConverter) {

			PropertyKey startNodeIdKey = getStartNodeIdKey();
			PropertyKey endNodeIdKey   = getEndNodeIdKey();

			if (startNodeIdKey != null && key.equals(startNodeIdKey)) {

				value = getStartNodeId();
				
				if(applyConverter) {
					
					cachedConvertedProperties.put(key, value);
					
				} else {
					
					cachedRawProperties.put(key, value);
				}
				
				return (T)value;

			}
			
			if (endNodeIdKey != null && key.equals(endNodeIdKey)) {

				value = getEndNodeId();
				
				if(applyConverter) {
					
					cachedConvertedProperties.put(key, value);
					
				} else {
					
					cachedRawProperties.put(key, value);
				}

				return (T)value;
				
			}
			
			// ----- BEGIN property group resolution -----
			PropertyGroup<T> propertyGroup = EntityContext.getPropertyGroup(type, key);

			if (propertyGroup != null) {

				try {
					return propertyGroup.getGroupedProperties(securityContext, this);
					
				} catch(FrameworkException fex) {
					
					logger.log(Level.WARNING, "Unable to convert property {0} of type {1}: {2}", new Object[] {
						key.dbName(),
						entityType.getSimpleName(),
						fex.getMessage()
					});
				}
			}

			if (dbRelationship.hasProperty(key.dbName())) {

				value = dbRelationship.getProperty(key.dbName());
			}

			// only apply converter if requested
			// (required for getComparableProperty())
			if(applyConverter) {

				PropertyConverter converter = key.databaseConverter(securityContext, this);
				if (converter != null) {

					try {

						value = converter.revert(value);
						
					} catch(Throwable t) {
						
						// CHM: remove debugging code later
						t.printStackTrace();
						
						logger.log(Level.WARNING, "Unable to convert property {0} of type {1}: {2}", new Object[] {
							key.dbName(),
							getClass().getSimpleName(),
							t.getMessage()
						});
					}
				}
			}

			if(!dontCache) {

				// only cache value if it is NOT the schema default
				if(applyConverter) {
					
					cachedConvertedProperties.put(key, value);
					
				} else {
					
					cachedRawProperties.put(key, value);
				}
			}
		}

		// no value found, use schema default
		if (value == null) {

			value = key.defaultValue();
			dontCache = true;
		}

		return (T)value;

	}

	@Override
	public Integer getIntProperty(final PropertyKey<Integer> key) {

		Object propertyValue = getProperty(key);
		Integer result       = null;

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof Integer) {

			result = ((Integer) propertyValue);
		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {

				return null;
			}

			result = Integer.parseInt(((String) propertyValue));

		}

		return result;

	}

	@Override
	public Long getLongProperty(final PropertyKey<Long> key) {

		Object propertyValue = getProperty(key);
		Long result          = null;

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof Long) {

			result = ((Long) propertyValue);
		} else if (propertyValue instanceof Integer) {

			result = ((Integer) propertyValue).longValue();
		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {

				return null;
			}

			result = Long.parseLong(((String) propertyValue));

		}

		return result;

	}

	@Override
	public Date getDateProperty(final PropertyKey<Date> key) {

		Object propertyValue = getProperty(key);

		if (propertyValue != null) {

			if (propertyValue instanceof Date) {

				return (Date) propertyValue;
			} else if (propertyValue instanceof Long) {

				return new Date((Long) propertyValue);
			} else if (propertyValue instanceof String) {

				try {

					// try to parse as a number
					return new Date(Long.parseLong((String) propertyValue));
				} catch (NumberFormatException nfe) {

					try {

						Date date = DateUtils.parseDate(((String) propertyValue), new String[] { "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss", "yyyymmdd", "yyyymm",
							"yyyy" });

						return date;

					} catch (ParseException ex2) {

						logger.log(Level.WARNING, "Could not parse " + propertyValue + " to date", ex2);

					}

					logger.log(Level.WARNING, "Can''t parse String {0} to a Date.", propertyValue);

					return null;

				}

			} else {

				logger.log(Level.WARNING, "Date property is not null, but type is neither Long nor String, returning null");

				return null;

			}

		}

		return null;

	}

	@Override
	public boolean getBooleanProperty(final PropertyKey<Boolean> key) {

		Object propertyValue = getProperty(key);
		Boolean result       = false;

		if (propertyValue == null) {

			return Boolean.FALSE;
		}

		if (propertyValue instanceof Boolean) {

			result = ((Boolean) propertyValue);
		} else if (propertyValue instanceof String) {

			result = Boolean.parseBoolean(((String) propertyValue));
		}

		return result;

	}

	@Override
	public Double getDoubleProperty(final PropertyKey<Double> key) throws FrameworkException {

		Object propertyValue = getProperty(key);
		Double result        = null;

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof Double) {

			Double doubleValue = (Double) propertyValue;

			if (doubleValue.equals(Double.NaN)) {

				// clean NaN values from database
				setProperty(key, null);

				return null;
			}

			result = doubleValue;

		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {

				return null;
			}

			result = Double.parseDouble(((String) propertyValue));

		}

		return result;

	}

	@Override
	public Comparable getComparableProperty(final PropertyKey<? extends Comparable> key) {

		Object propertyValue = getProperty(key, false);	// get "raw" property without converter
		Class type = getClass();
		
		// check property converter
//		PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);
		PropertyConverter converter = key.databaseConverter(securityContext, this);
		if (converter != null) {
			
			try {
				return converter.convertForSorting(propertyValue);

			} catch(FrameworkException fex) {
				logger.log(Level.WARNING, "Unable to convert property {0} of type {1}: {2}", new Object[] {
					key.dbName(),
					getClass().getSimpleName(),
					fex.getMessage()
				});
			}
		}
		
		// conversion failed, may the property value itself is comparable
		if(propertyValue instanceof Comparable) {
			return (Comparable)propertyValue;
		}
		
		// last try: convertFromInput to String to make comparable
		if(propertyValue != null) {
			return propertyValue.toString();
		}
		
		return null;
	}

	/**
	 * Return database relationship
	 *
	 * @return
	 */
	public Relationship getRelationship() {

		return dbRelationship;

	}

	public AbstractNode getEndNode() {

		NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		return (AbstractNode) nodeFactory.createNode(dbRelationship.getEndNode());

	}

	public AbstractNode getStartNode() {

		NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		return (AbstractNode) nodeFactory.createNode(dbRelationship.getStartNode());
	}

	public AbstractNode getOtherNode(final AbstractNode node) {

		NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		return (AbstractNode) nodeFactory.createNode(dbRelationship.getOtherNode(node.getNode()));

	}

	public RelationshipType getRelType() {

		return dbRelationship.getType();

	}

	public String[] getPermissions() {

		if (dbRelationship.hasProperty(AbstractRelationship.allowed.dbName())) {

			// StringBuilder result             = new StringBuilder();
			String[] allowedProperties = (String[]) dbRelationship.getProperty(AbstractRelationship.allowed.dbName());

			return allowedProperties;

//                      if (allowedProperties != null) {
//
//                              for (String p : allowedProperties) {
//
//                                      result.append(p).append("\n");
//
//                              }
//
//                      }
//
//                      return result.toString();
		} else {

			return null;
		}

	}

	/**
	 * Return all property keys.
	 *
	 * @return
	 */
	public Iterable<PropertyKey> getPropertyKeys() {

		return getPropertyKeys(PropertyView.All);

	}

	/**
	 * Return property value which is used for indexing.
	 *
	 * This is useful f.e. to filter markup from HTML to index only text
	 *
	 * @param key
	 * @return
	 */
	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		return getProperty(key);

	}

	// ----- interface GraphObject -----
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		return EntityContext.getPropertySet(this.getClass(), propertyView);

	}

	public Map<RelationshipType, Long> getRelationshipInfo(Direction direction) {

		return null;

	}

	public List<AbstractRelationship> getRelationships(RelationshipType type, Direction dir) {

		return null;

	}

	@Override
	public String getType() {

		return getRelType().name();

	}
	
	@Override
	public PropertyContainer getPropertyContainer() {
		return dbRelationship;
	}

	public String getStartNodeId() {

		return getStartNode().getUuid();

	}

	public String getEndNodeId() {

		return getEndNode().getUuid();

	}

	public String getOtherNodeId(final AbstractNode node) {

		return getOtherNode(node).getProperty(AbstractRelationship.uuid);

	}

	private AbstractNode getNodeByUuid(final String uuid) throws FrameworkException {

		return (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute(uuid);

	}

	public String getCachedStartNodeId() {

		return cachedStartNodeId;

	}

	public String getCachedEndNodeId() {

		return cachedEndNodeId;

	}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		
		cachedUuid = (String)properties.get(AbstractRelationship.uuid);
		
		return true;
	}
	
	@Override
	public void afterCreation(SecurityContext securityContext) {
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
	}

	@Override
	public void afterDeletion(SecurityContext securityContext) {
	}

	@Override
	public void ownerModified(SecurityContext securityContext) {
	}
	
	@Override
	public void securityModified(SecurityContext securityContext) {
	}
	
	@Override
	public void locationModified(SecurityContext securityContext) {
	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {
	}
	
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

		error |= ValidationHelper.checkStringNotBlank(this, AbstractRelationship.uuid, errorBuffer);

		return !error;

	}

	public boolean isType(RelType type) {

		return ((type != null) && type.equals(dbRelationship.getType()));

	}

	public boolean isAllowed(final Permission permission) {

		if (dbRelationship.hasProperty(allowed.dbName())) {

			String[] allowedProperties = (String[]) dbRelationship.getProperty(allowed.dbName());

			if (allowedProperties != null) {

				for (String p : allowedProperties) {

					if (p.equals(permission.name())) {

						return true;
					}

				}

			}

		}

		return false;

	}

	//~--- set methods ----------------------------------------------------

	public void setProperties(final PropertyMap properties) throws FrameworkException {

		for (Entry<PropertyKey, Object> prop : properties.entrySet()) {

			setProperty(prop.getKey(), prop.getValue());
		}

	}

	@Override
	public <T> void setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {

		PropertyKey startNodeIdKey = getStartNodeIdKey();
		PropertyKey endNodeIdKey   = getEndNodeIdKey();

		// clear cached property
		cachedConvertedProperties.remove(key);
		cachedRawProperties.remove(key);
		
		if ((startNodeIdKey != null) && key.equals(startNodeIdKey)) {

			setStartNodeId((String) value);

			return;

		}

		if ((endNodeIdKey != null) && key.equals(endNodeIdKey)) {

			setEndNodeId((String) value);

			return;

		}

		Class type = this.getClass();

		// check for read-only properties
		//if (EntityContext.isReadOnlyProperty(type, key) || (EntityContext.isWriteOnceProperty(type, key) && (dbRelationship != null) && dbRelationship.hasProperty(key.name()))) {
		if (key.isReadOnlyProperty() || (key.isWriteOnceProperty() && (dbRelationship != null) && dbRelationship.hasProperty(key.dbName()))) {

			if (readOnlyPropertiesUnlocked) {

				// permit write operation once and
				// lock scanEntity-only properties again
				readOnlyPropertiesUnlocked = false;
				
			} else {

				throw new FrameworkException(type.getSimpleName(), new ReadOnlyPropertyToken(key));
			}

		}

		// ----- BEGIN property group resolution -----
		PropertyGroup propertyGroup = EntityContext.getPropertyGroup(type, key);

		if (propertyGroup != null) {

			propertyGroup.setGroupedProperties(securityContext, value, this);

			return;

		}

//		PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);
		PropertyConverter converter = key.databaseConverter(securityContext, this);
		final Object convertedValue;

		if (converter != null) {

			convertedValue = converter.convert(value);

		} else {

			convertedValue = value;
		}

		final Object oldValue = getProperty(key);
		
		// don't make any changes if
		// - old and new value both are null
		// - old and new value are not null but equal
		if (((convertedValue == null) && (oldValue == null)) || ((convertedValue != null) && (oldValue != null) && convertedValue.equals(oldValue))) {

			return;
		}

		// Commit value directly to database
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				try {

					// save space
					if (convertedValue == null) {

						dbRelationship.removeProperty(key.dbName());
						
					} else {

						dbRelationship.setProperty(key.dbName(), convertedValue);
					}
					
				} finally {}

				return null;

			}

		};

		// execute transaction
		Services.command(securityContext, TransactionCommand.class).execute(transaction);

		// clear cached property
		cachedConvertedProperties.remove(key);
		cachedRawProperties.remove(key);
	}

	/**
	 * Set node id of start node.
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, ends at the same end node,
	 * but starting from the node with startNodeId
	 *
	 */
	public void setStartNodeId(final String startNodeId) throws FrameworkException {

		final String type     = this.getClass().getSimpleName();
		final PropertyKey key = getStartNodeIdKey();

		// May never be null!!
		if (startNodeId == null) {

			throw new FrameworkException(type, new NullPropertyToken(key));
		}
		
		// Do nothing if new id equals old
		if (getStartNodeId().equals(startNodeId)) {
			return;
		}

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
				CreateRelationshipCommand createRel = Services.command(securityContext, CreateRelationshipCommand.class);
				AbstractNode newStartNode           = getNodeByUuid(startNodeId);
				AbstractNode endNode                = getEndNode();

				if (newStartNode == null) {

					throw new FrameworkException(type, new IdNotFoundToken(startNodeId));
				}

				RelationshipType type = dbRelationship.getType();

				properties = getProperties();

				deleteRel.execute(dbRelationship);

				AbstractRelationship newRel = (AbstractRelationship) createRel.execute(newStartNode, endNode, type, properties, false);

				dbRelationship = newRel.getRelationship();

				return (null);

			}

		});

	}

	/**
	 * Set node id of end node.
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, start from the same start node,
	 * but pointing to the node with endNodeId
	 *
	 */
	public void setEndNodeId(final String endNodeId) throws FrameworkException {

		final String type     = this.getClass().getSimpleName();
		final PropertyKey key = getStartNodeIdKey();

		// May never be null!!
		if (endNodeId == null) {

			throw new FrameworkException(type, new NullPropertyToken(key));
		}
		
		// Do nothing if new id equals old
		if (getEndNodeId().equals(endNodeId)) {
			return;
		}

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
				CreateRelationshipCommand createRel = Services.command(securityContext, CreateRelationshipCommand.class);
				AbstractNode startNode              = getStartNode();
				AbstractNode newEndNode             = getNodeByUuid(endNodeId);

				if (newEndNode == null) {

					throw new FrameworkException(type, new IdNotFoundToken(endNodeId));
				}

				RelationshipType type = dbRelationship.getType();

				properties = getProperties();

				deleteRel.execute(dbRelationship);

				AbstractRelationship newRel = (AbstractRelationship) createRel.execute(startNode, newEndNode, type, properties, false);

				dbRelationship = newRel.getRelationship();

				return (null);

			}

		});

	}

	/**
	 * Set relationship combinedType
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, with the same start and end node,
	 * but with another combinedType
	 *
	 */
	public void setType(final String type) {

		if (type != null) {

			try {

				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
						CreateRelationshipCommand createRel = Services.command(securityContext, CreateRelationshipCommand.class);
						AbstractNode startNode = getStartNode();
						AbstractNode endNode   = getEndNode();

						deleteRel.execute(dbRelationship);

						dbRelationship = createRel.execute(startNode, endNode, type).getRelationship();

						return (null);

					}

				});

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to set relationship type", fex);

			}

		}

	}

	public void setAllowed(final List<String> allowed) {

		String[] allowedActions = (String[]) allowed.toArray(new String[allowed.size()]);

		setAllowed(allowedActions);

	}

	public void setAllowed(final Permission[] allowed) {

		List<String> allowedActions = new ArrayList<String>();

		for (Permission permission : allowed) {

			allowedActions.add(permission.name());
		}

		setAllowed(allowedActions);

	}

	public void setAllowed(final String[] allowed) {

		dbRelationship.setProperty(AbstractRelationship.allowed.dbName(), allowed);

	}
}
