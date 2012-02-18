/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

import java.util.*;
import org.neo4j.graphdb.*;

import org.structr.common.PropertyKey;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.common.UuidCreationTransformation;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.EntityContext;
import org.structr.core.PropertyConverter;
import org.structr.core.PropertyGroup;
import org.structr.core.Value;
import org.structr.core.cloud.RelationshipDataContainer;
import org.structr.core.node.NodeService.RelationshipIndex;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public abstract class AbstractRelationship implements GraphObject, Comparable<AbstractRelationship> {

	private static final Logger logger = Logger.getLogger(AbstractRelationship.class.getName());

//      public final static String ALLOWED_KEY = "allowed";
//      public final static String DENIED_KEY = "denied";
//      public final static String READ_KEY = "read";
//      public final static String SHOW_TREE_KEY = "showTree";
//      public final static String WRITE_KEY = "write";
//      public final static String EXECUTE_KEY = "execute";
//      public final static String CREATE_SUBNODE_KEY = "createNode";
//      public final static String DELETE_NODE_KEY = "deleteNode";
//      public final static String EDIT_PROPERTIES_KEY = "editProperties";
//      public final static String ADD_RELATIONSHIP_KEY = "addRelationship";
//      public final static String REMOVE_RELATIONSHIP_KEY = "removeRelationship";
//      public final static String ACCESS_CONTROL_KEY = "accessControl";
//      public final static String[] ALL_PERMISSIONS = new String[]{
//              READ_KEY, SHOW_TREE_KEY,
//              CREATE_SUBNODE_KEY, DELETE_NODE_KEY,
//              WRITE_KEY, EXECUTE_KEY,
//              ADD_RELATIONSHIP_KEY, REMOVE_RELATIONSHIP_KEY,
//              EDIT_PROPERTIES_KEY, ACCESS_CONTROL_KEY
//      };
	protected SecurityContext securityContext = null;

	// reference to database relationship
	protected Relationship dbRelationship;

	protected Map<String, Object> properties;
	protected boolean isDirty;

	private boolean readOnlyPropertiesUnlocked                   = false;

	static {

		EntityContext.registerPropertySet(AbstractRelationship.class, PropertyView.All, Key.values());

		EntityContext.registerSearchablePropertySet(AbstractRelationship.class, RelationshipIndex.rel_uuid.name(), Key.uuid);

		// register transformation for automatic uuid creation
		EntityContext.registerEntityCreationTransformation(AbstractRelationship.class, new UuidCreationTransformation());

	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {
		uuid
	}

	public enum Permission implements PropertyKey {
		allowed, denied, read, showTree, write, execute, createNode, deleteNode, editProperties, addRelationship, removeRelationship, accessControl;
	}

	//~--- constructors ---------------------------------------------------

	public AbstractRelationship() {
		this.properties = new HashMap<String, Object>();
		isDirty         = true;
	}

	public AbstractRelationship(final Map<String, Object> properties) {

		this.properties = properties;
		isDirty         = true;
	}

	public AbstractRelationship(final SecurityContext securityContext, final Relationship dbRel) {
		init(securityContext, dbRel);
	}

	public AbstractRelationship(final SecurityContext securityContext, final RelationshipDataContainer data) {

		if (data != null) {

			this.securityContext = securityContext;
			this.properties      = data.getProperties();
			this.isDirty = true;

		}
	}
	//~--- methods --------------------------------------------------------
/**
	 * Commit unsaved property values to the relationship node.
	 */
	public void commit() throws FrameworkException {

		isDirty = false;

		// Create an outer transaction to combine any inner neo4j transactions
		// to one single transaction
		Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

		transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);
				AbstractRelationship rel     = (AbstractRelationship) createRel.execute();

				init(securityContext, rel.getRelationship());

				Set<String> keys = properties.keySet();

				for (String key : keys) {

					Object value = properties.get(key);

					if ((key != null) && (value != null)) {

						setProperty(key, value);

					}

				}

				return null;
			}

		});
	}
	
	public void init(final SecurityContext securityContext, final Relationship dbRel) {

		this.dbRelationship          = dbRel;
		this.isDirty         = false;
		this.securityContext = securityContext;

	}

	private void init(final SecurityContext securityContext, final AbstractRelationship rel) {

		this.dbRelationship          = rel.dbRelationship;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	public void init(final SecurityContext securityContext, final RelationshipDataContainer data) {

		if (data != null) {

			this.properties      = data.getProperties();
			this.isDirty         = true;
			this.securityContext = securityContext;

		}
	}

	public void unlockReadOnlyPropertiesOnce() {
		this.readOnlyPropertiesUnlocked = true;
	}

	@Override
	public void removeProperty(final String key) {
		dbRelationship.removeProperty(key);
	}

	@Override
	public boolean equals(final Object o) {
		return (new Integer(this.hashCode()).equals(new Integer(o.hashCode())));
	}

	@Override
	public int hashCode() {

		if (this.dbRelationship == null) {

			return (super.hashCode());

		}

		return (new Long(dbRelationship.getId()).hashCode());
	}

	@Override
	public void delete(SecurityContext securityContext) {

		dbRelationship.delete();

		// EntityContext.getGlobalModificationListener().relationshipDeleted(securityContext, this);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public long getId() {
		return getInternalId();
	}

	public long getRelationshipId() {
		return getInternalId();
	}

	public long getInternalId() {
		return dbRelationship.getId();
	}

	public Map<String, Object> getProperties() {

		Map<String, Object> properties = new HashMap<String, Object>();

		for (String key : dbRelationship.getPropertyKeys()) {

			properties.put(key, dbRelationship.getProperty(key));

		}

		return properties;
	}

	public Object getProperty(final PropertyKey propertyKey) {
		return getProperty(propertyKey.name());
	}

	@Override
	public Object getProperty(final String key) {

		Class type = this.getClass();
		Object value = null;

		// ----- BEGIN property group resolution -----
		PropertyGroup propertyGroup = EntityContext.getPropertyGroup(type, key);
		if (propertyGroup != null) {

			return propertyGroup.getGroupedProperties(this);
		}

		if(dbRelationship.hasProperty(key)) {
			value = dbRelationship.getProperty(key);

		}

		// apply property converters
		PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);

		if (converter != null) {

			Value conversionValue = EntityContext.getPropertyConversionParameter(type, key);
			value = converter.convertForGetter(value, conversionValue);

		}

		return value;
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

		try {
			Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
			return (AbstractNode) nodeFactory.execute(dbRelationship.getEndNode());

		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to instantiate node", fex);
		}
		return null;
	}

	public AbstractNode getStartNode() {

		try {
			Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
			return (AbstractNode) nodeFactory.execute(dbRelationship.getStartNode());

		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to instantiate node", fex);
		}
		return null;
	}

	public AbstractNode getOtherNode(final AbstractNode node) {

		try {
			Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
			return (AbstractNode) nodeFactory.execute(dbRelationship.getOtherNode(node.getNode()));

		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to instantiate node", fex);
		}
		return null;
	}

	public RelationshipType getRelType() {
		return (dbRelationship.getType());
	}

	public String getAllowed() {

		if (dbRelationship.hasProperty(AbstractRelationship.Permission.allowed.name())) {

			String result              = "";
			String[] allowedProperties = (String[]) dbRelationship.getProperty(AbstractRelationship.Permission.allowed.name());

			if (allowedProperties != null) {

				for (String p : allowedProperties) {

					result += p + "\n";

				}

			}

			return result;

		} else {

			return null;

		}
	}

	public String getDenied() {

		if (dbRelationship.hasProperty(AbstractRelationship.Permission.denied.name())) {

			String result             = "";
			String[] deniedProperties = (String[]) dbRelationship.getProperty(AbstractRelationship.Permission.denied.name());

			if (deniedProperties != null) {

				for (String p : deniedProperties) {

					result += p + "\n";

				}

			}

			return result;

		} else {

			return null;

		}
	}
	/**
	 * Return all property keys.
	 *
	 * @return
	 */
	public Iterable<String> getPropertyKeys() {
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
	public Object getPropertyForIndexing(final String key) {
		return getProperty(key);
	}
	
	// ----- interface GraphObject -----
	@Override
	public Iterable<String> getPropertyKeys(final String propertyView) {
		return EntityContext.getPropertySet(this.getClass(), propertyView);
	}

	@Override
	public Map<RelationshipType, Long> getRelationshipInfo(Direction direction) {
		return null;
	}

	@Override
	public List<AbstractRelationship> getRelationships(RelationshipType type, Direction dir) {
		return null;
	}

	@Override
	public String getType() {
		return this.getRelType().name();
	}

	@Override
	public Long getStartNodeId() {
		return this.getStartNode().getId();
	}

	@Override
	public Long getEndNodeId() {
		return this.getEndNode().getId();
	}

	@Override
	public Long getOtherNodeId(final AbstractNode node) {
		return this.getOtherNode(node).getId();
	}

	public boolean isType(RelType type) {
		return ((type != null) && type.equals(dbRelationship.getType()));
	}

	public boolean isAllowed(final String action) {

		if (dbRelationship.hasProperty(AbstractRelationship.Permission.allowed.name())) {

			String[] allowedProperties = (String[]) dbRelationship.getProperty(AbstractRelationship.Permission.allowed.name());

			if (allowedProperties != null) {

				for (String p : allowedProperties) {

					if (p.equals(action)) {

						return true;

					}

				}

			}

		}

		return false;
	}

	public boolean isDenied(final String action) {

		if (dbRelationship.hasProperty(AbstractRelationship.Permission.denied.name())) {

			String[] deniedProperties = (String[]) dbRelationship.getProperty(AbstractRelationship.Permission.denied.name());

			if (deniedProperties != null) {

				for (String p : deniedProperties) {

					if (p.equals(action)) {

						return true;

					}

				}

			}

		}

		return false;
	}

	//~--- set methods ----------------------------------------------------

	public void setProperties(final Map<String, Object> properties) throws FrameworkException {

		for (String key : properties.keySet()) {

			setProperty(key, properties.get(key));

		}
	}

	public void setProperty(final PropertyKey propertyKey, final Object value) throws FrameworkException {
		setProperty(propertyKey.name(), value);
	}

	@Override
	public void setProperty(final String key, final Object value) throws FrameworkException {

		Class type = this.getClass();

		// check for read-only properties
		if (EntityContext.isReadOnlyProperty(type, key) || (EntityContext.isWriteOnceProperty(type, key) && (dbRelationship != null) && dbRelationship.hasProperty(key))) {

			if (readOnlyPropertiesUnlocked) {

				// permit write operation once and
				// lock read-only properties again
				readOnlyPropertiesUnlocked = false;

			} else {
				throw new FrameworkException(type.getSimpleName(), new ReadOnlyPropertyToken(key));
			}

		}

		// ----- BEGIN property group resolution -----
		PropertyGroup propertyGroup = EntityContext.getPropertyGroup(type, key);

		if (propertyGroup != null) {

			propertyGroup.setGroupedProperties(value, this);

			return;

		}

		PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);
		final Object convertedValue;

		if (converter != null) {

			Value conversionValue = EntityContext.getPropertyConversionParameter(type, key);
			convertedValue = converter.convertForSetter(value, conversionValue);

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

						dbRelationship.removeProperty(key);

					} else {

						if (convertedValue instanceof Date) {

							dbRelationship.setProperty(key, ((Date) convertedValue).getTime());

						} else {

							dbRelationship.setProperty(key, convertedValue);

						}
					}


				} finally {}

				return null;
			}
		};

		// execute transaction
		Services.command(securityContext, TransactionCommand.class).execute(transaction);
	}

	/**
	 * Set node id of end node.
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, start from the same start node,
	 * but pointing to the node with endNodeId
	 *
	 */
	public void setEndNodeId(final User user, final long endNodeId) {

		Command transaction = Services.command(securityContext, TransactionCommand.class);

		try {
			transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					Command findNode        = Services.command(securityContext, FindNodeCommand.class);
					Command deleteRel       = Services.command(securityContext, DeleteRelationshipCommand.class);
					Command createRel       = Services.command(securityContext, CreateRelationshipCommand.class);
					Command nodeFactory     = Services.command(securityContext, NodeFactoryCommand.class);
					AbstractNode startNode  = (AbstractNode) nodeFactory.execute(getStartNode());
					AbstractNode newEndNode = (AbstractNode) findNode.execute(endNodeId);

					if (newEndNode != null) {

						RelationshipType type = dbRelationship.getType();

						deleteRel.execute(dbRelationship);

						dbRelationship = ((AbstractRelationship) createRel.execute(type, startNode, newEndNode)).getRelationship();

					}

					return (null);
				}

			});

		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to set end node id", fex);
		}
	}

	/**
	 * Set relationship type
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, with the same start and end node,
	 * but with another type
	 *
	 */
	public void setType(final String type) {

		if (type != null) {

			try {
				Command transacted = Services.command(securityContext, TransactionCommand.class);
				transacted.execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Command deleteRel      = Services.command(securityContext, DeleteRelationshipCommand.class);
						Command createRel      = Services.command(securityContext, CreateRelationshipCommand.class);
						Command nodeFactory    = Services.command(securityContext, NodeFactoryCommand.class);
						AbstractNode startNode = (AbstractNode) nodeFactory.execute(getStartNode());
						AbstractNode endNode   = (AbstractNode) nodeFactory.execute(getEndNode());

						deleteRel.execute(dbRelationship);

						dbRelationship = ((AbstractRelationship) createRel.execute(type, startNode, endNode)).getRelationship();

						return (null);
					}

				});

			} catch(FrameworkException fex) {
				logger.log(Level.WARNING, "Unable to set relationship type", fex);
			}
		}
	}

	public void setAllowed(final List<String> allowed) {

		String[] allowedActions = (String[]) allowed.toArray(new String[allowed.size()]);

		dbRelationship.setProperty(AbstractRelationship.Permission.allowed.name(), allowedActions);
	}

	public void setAllowed(final PropertyKey[] allowed) {

		List<String> allowedActions = new ArrayList<String>();

		for (PropertyKey key : allowed) {

			allowedActions.add(key.name());

		}

		setAllowed(allowedActions);
	}

	public void setDenied(final List<String> denied) {

		if (dbRelationship.hasProperty(AbstractRelationship.Permission.denied.name())) {

			dbRelationship.setProperty(AbstractRelationship.Permission.denied.name(), denied);

		}
	}

	@Override
	public int compareTo(final AbstractRelationship rel) {

		// TODO: implement finer compare methods, e.g. taking title and position into account
		if (rel == null) {

			return -1;

		}

		return ((Long) this.getId()).compareTo((Long) rel.getId());
	}
}
