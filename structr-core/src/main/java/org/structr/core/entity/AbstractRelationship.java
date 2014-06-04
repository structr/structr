/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

//~--- classes ----------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.GraphObject;
import static org.structr.core.GraphObject.id;
import static org.structr.core.GraphObject.type;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationshipTypeProperty;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;


/**
 * Abstract base class for all relationship entities in structr.
 *
 * @author Axel Morgner
 */
public abstract class AbstractRelationship<S extends NodeInterface, T extends NodeInterface> implements Comparable<AbstractRelationship>, RelationshipInterface {

	private static final Logger logger = Logger.getLogger(AbstractRelationship.class.getName());

	public static final Property<Integer> cascadeDelete = new IntProperty("cascadeDelete").writeOnce();
	public static final Property<String>  relType       = new RelationshipTypeProperty("relType");
	public static final SourceId          sourceId      = new SourceId("sourceId");
	public static final TargetId          targetId      = new TargetId("targetId");

	public static final View defaultView = new View(AbstractRelationship.class, PropertyView.Public,
		id, type, relType, sourceId, targetId
	);

	public static final View uiView = new View(AbstractRelationship.class, PropertyView.Ui,
		id, type, relType, sourceId, targetId
	);

	private boolean readOnlyPropertiesUnlocked = false;
	private String cachedEndNodeId             = null;
	private String cachedStartNodeId           = null;

	protected SecurityContext securityContext  = null;
	protected Relationship dbRelationship      = null;
	protected PropertyMap properties           = null;
	protected Class entityType                 = getClass();
	protected String cachedUuid                = null;
	protected boolean isDirty                  = false;

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

	@Override
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

	public Property<String> getSourceIdProperty() {
		return sourceId;
	}

	public Property<String> getTargetIdProperty() {
		return null;
	}

	@Override
	public void onRelationshipCreation() {
	}

	/**
	 * Called when a relationship of this combinedType is instatiated. Please note that
	 * a relationship can (and will) be instantiated several times during a
	 * normal rendering turn.
	 */
	@Override
	public void onRelationshipInstantiation() {

		try {

			if (dbRelationship != null) {

				Node startNode = dbRelationship.getStartNode();
				Node endNode   = dbRelationship.getEndNode();

				if ((startNode != null) && (endNode != null) && startNode.hasProperty(GraphObject.id.dbName()) && endNode.hasProperty(GraphObject.id.dbName())) {

					cachedStartNodeId = (String) startNode.getProperty(GraphObject.id.dbName());
					cachedEndNodeId   = (String) endNode.getProperty(GraphObject.id.dbName());

				}

			}

		} catch (Throwable t) {
		}
	}

	@Override
	public void onRelationshipDeletion() {
	}

	@Override
	public void setSecurityContext(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public SecurityContext getSecurityContext() {
		return this.securityContext;
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {

		this.readOnlyPropertiesUnlocked = true;

	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {

		dbRelationship.removeProperty(key.dbName());

		// remove from index
		removeFromIndex(key);
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

	@Override
	public int cascadeDelete() {

		Integer value = getProperty(AbstractRelationship.cascadeDelete);

		return value != null ? value : 0;
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

	@Override
	public long getId() {

		return getInternalId();

	}

	@Override
	public String getUuid() {

		return getProperty(AbstractRelationship.id);

	}

	public long getRelationshipId() {

		return getInternalId();

	}

	public long getInternalId() {

		return dbRelationship.getId();

	}

	@Override
	public PropertyMap getProperties() throws FrameworkException {

		Map<String, Object> properties = new LinkedHashMap<>();

		for (String key : dbRelationship.getPropertyKeys()) {

			properties.put(key, dbRelationship.getProperty(key));
		}

		// convert the database properties back to their java types
		return PropertyMap.databaseTypeToJavaType(securityContext, this, properties);

	}

	@Override
	public <T> T getProperty(final PropertyKey<T> key) {
		return getProperty(key, true, null);
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> key, final org.neo4j.helpers.Predicate<GraphObject> predicate) {
		return getProperty(key, true, predicate);
	}

	private <T> T getProperty(final PropertyKey<T> key, boolean applyConverter, final org.neo4j.helpers.Predicate<GraphObject> predicate) {

		// early null check, this should not happen...
		if (key == null || key.dbName() == null) {
			return null;
		}

		return key.getProperty(securityContext, this, applyConverter, predicate);
	}

	@Override
	public <T> Comparable getComparableProperty(final PropertyKey<T> key) {

		if (key != null) {

			final T propertyValue = getProperty(key, false, null);	// get "raw" property without converter

			// check property converter
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
		}

		return null;
	}

	/**
	 * Return database relationship
	 *
	 * @return
	 */
	@Override
	public Relationship getRelationship() {

		return dbRelationship;

	}

	@Override
	public T getTargetNode() {

		try {

			NodeFactory<T> nodeFactory = new NodeFactory<>(securityContext);
			return nodeFactory.instantiate(dbRelationship.getEndNode());

		} catch (FrameworkException t) {
			// ignore FrameworkException but let NotInTransactionException pass
		}

		return null;
	}

	@Override
	public S getSourceNode() {

		try {

			NodeFactory<S> nodeFactory = new NodeFactory<>(securityContext);
			return nodeFactory.instantiate(dbRelationship.getStartNode());

		} catch (FrameworkException t) {
			// ignore FrameworkException but let NotInTransactionException pass
		}

		return null;
	}

	@Override
	public NodeInterface getOtherNode(final NodeInterface node) {

		try {

			NodeFactory nodeFactory = new NodeFactory(securityContext);
			return (NodeInterface)nodeFactory.instantiate(dbRelationship.getOtherNode(node.getNode()));

		} catch (FrameworkException t) {
			// ignore FrameworkException but let NotInTransactionException pass
		}

		return null;
	}

	@Override
	public RelationshipType getRelType() {

		if (dbRelationship != null) {

			return dbRelationship.getType();
		}

		return null;
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

		Object value = getProperty(key, false, null);
		if (value != null) {
			return value;
		}

		return getProperty(key);
	}

	// ----- interface GraphObject -----
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		return StructrApp.getConfiguration().getPropertySet(this.getClass(), propertyView);

	}

	public Map<RelationshipType, Long> getRelationshipInfo(Direction direction) {

		return null;

	}

	public List<AbstractRelationship> getRelationships(RelationshipType type, Direction dir) {

		return null;

	}

	@Override
	public String getType() {

		final RelationshipType relType = getRelType();
		if (relType != null) {
			return relType.name();
		}

		return null;
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return dbRelationship;
	}

	@Override
	public String getSourceNodeId() {
		return cachedStartNodeId;
	}

	@Override
	public String getTargetNodeId() {
		return cachedEndNodeId;

	}

	public String getOtherNodeId(final AbstractNode node) {

		return getOtherNode(node).getProperty(AbstractRelationship.id);

	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		return true;
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
	}

	@Override
	public void afterDeletion(SecurityContext securityContext, PropertyMap properties) {
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

		error |= ValidationHelper.checkStringNotBlank(this, AbstractRelationship.id, errorBuffer);

		return !error;

	}

	//~--- set methods ----------------------------------------------------

	public void setProperties(final PropertyMap properties) throws FrameworkException {

		for (Entry<PropertyKey, Object> prop : properties.entrySet()) {

			setProperty(prop.getKey(), prop.getValue());
		}

	}

	@Override
	public <T> void setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {

		// check for read-only properties
		//if (StructrApp.getConfiguration().isReadOnlyProperty(type, key) || (StructrApp.getConfiguration().isWriteOnceProperty(type, key) && (dbRelationship != null) && dbRelationship.hasProperty(key.name()))) {
		if (key.isReadOnly() || (key.isWriteOnce() && (dbRelationship != null) && dbRelationship.hasProperty(key.dbName()))) {

			if (readOnlyPropertiesUnlocked || securityContext.isSuperUser()) {

				// permit write operation once and
				// lock read-only properties again
				readOnlyPropertiesUnlocked = false;

			} else {

				throw new FrameworkException(getClass().getSimpleName(), new ReadOnlyPropertyToken(key));
			}

		}

		key.setProperty(securityContext, this, value);
	}

	@Override
	public void addToIndex() {

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(entityType, PropertyView.All)) {

			if (key.isIndexed()) {

				key.index(this, this.getPropertyForIndexing(key));
			}
		}
	}

	@Override
	public void updateInIndex() {

		removeFromIndex();
		addToIndex();
	}

	@Override
	public void removeFromIndex() {

		for (Index<Relationship> index : Services.getInstance().getService(NodeService.class).getRelationshipIndices()) {

			synchronized (index) {

				index.remove(dbRelationship);
			}
		}
	}

	public void removeFromIndex(PropertyKey key) {

		for (Index<Relationship> index : Services.getInstance().getService(NodeService.class).getRelationshipIndices()) {

			synchronized (index) {

				index.remove(dbRelationship, key.dbName());
			}
		}
	}

	@Override
	public void indexPassiveProperties() {

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(entityType, PropertyView.All)) {

			if (key.isPassivelyIndexed()) {

				key.index(this, this.getPropertyForIndexing(key));
			}
		}

	}

	@Override
	public void setSourceNodeId(final String startNodeId) throws FrameworkException {

		// Do nothing if new id equals old
		if (getSourceNodeId().equals(startNodeId)) {
			return;
		}

		final App app = StructrApp.getInstance(securityContext);

		final NodeInterface newStartNode = (NodeInterface)app.get(startNodeId);
		final NodeInterface endNode      = getTargetNode();
		final Class relationType         = getClass();
		final PropertyMap _props         = getProperties();
		final String type                = this.getClass().getSimpleName();

		if (newStartNode == null) {
			throw new FrameworkException(type, new IdNotFoundToken(startNodeId));
		}

		// delete this as the new rel will be the container afterwards
		app.delete(this);

		// create new relationship
		app.create(newStartNode, endNode, relationType, _props);
	}

	@Override
	public void setTargetNodeId(final String targetIdNode) throws FrameworkException {

		// Do nothing if new id equals old
		if (getTargetNodeId().equals(targetIdNode)) {
			return;
		}

		final App app = StructrApp.getInstance(securityContext);

		final NodeInterface newTargetNode = (NodeInterface)app.get(targetIdNode);
		final NodeInterface startNode     = getSourceNode();
		final Class relationType          = getClass();
		final PropertyMap _props          = getProperties();
		final String type                 = this.getClass().getSimpleName();

		if (newTargetNode == null) {
			throw new FrameworkException(type, new IdNotFoundToken(targetIdNode));
		}

		// delete this as the new rel will be the container afterwards
		app.delete(this);

		// create new relationship and store here
		app.create(startNode, newTargetNode, relationType, _props);
	}

	@Override
	public String getPropertyWithVariableReplacement(SecurityContext securityContext, ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {
		return SchemaHelper.getPropertyWithVariableReplacement(securityContext, this, renderContext, key);
	}

	@Override
	public String replaceVariables(final SecurityContext securityContext, final ActionContext actionContext, final Object rawValue) throws FrameworkException {
		return SchemaHelper.replaceVariables(securityContext, this, actionContext, rawValue);
	}

	// ----- protected methods -----
	protected Direction getDirectionForType(final Class<S> sourceType, final Class<T> targetType, final Class<? extends NodeInterface> type) {

		if (sourceType.equals(type) && targetType.equals(type)) {
			return Direction.BOTH;
		}

		if (sourceType.equals(type)) {
			return Direction.OUTGOING;
		}

		if (targetType.equals(type)) {
			return Direction.INCOMING;
		}

		if (sourceType.isAssignableFrom(type)) {
			return Direction.OUTGOING;
		}

		if (targetType.isAssignableFrom(type)) {
			return Direction.INCOMING;
		}

		return Direction.BOTH;
	}
}
