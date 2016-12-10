/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.NativeResult;
import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Path;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.index.Index;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.cmis.common.StructrItemActions;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.cmis.info.CMISItemInfo;
import org.structr.cmis.info.CMISPolicyInfo;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.cmis.info.CMISSecondaryInfo;
import org.structr.common.AccessControllable;
import org.structr.common.AccessPathCache;
import org.structr.common.GraphObjectComparator;
import org.structr.common.IdSorter;
import org.structr.common.Permission;
import org.structr.common.PermissionPropagation;
import org.structr.common.PermissionResolutionMask;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InternalSystemPropertyToken;
import org.structr.common.error.NullArgumentToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.relationship.Ownership;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeRelationshipStatisticsCommand;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

//~--- classes ----------------------------------------------------------------
/**
 * Abstract base class for all node entities in structr.
 *
 *
 *
 */
public abstract class AbstractNode implements NodeInterface, AccessControllable, CMISInfo, CMISItemInfo {

	private static final FixedSizeCache<String, Object> relationshipTemplateInstanceCache = new FixedSizeCache<>(1000);
	private static final Logger logger = LoggerFactory.getLogger(AbstractNode.class.getName());

	public static final View defaultView = new View(AbstractNode.class, PropertyView.Public, id, type);

	public static final View uiView = new View(AbstractNode.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate
	);

	public boolean internalSystemPropertiesUnlocked = false;

	private PermissionResolutionMask permissionResolutionMask = null;
	private Relationship rawPathSegment                       = null;

	private boolean readOnlyPropertiesUnlocked       = false;
	private boolean isCreation                       = false;

	protected String cachedUuid               = null;
	protected SecurityContext securityContext = null;
	protected Principal cachedOwnerNode       = null;
	protected Class entityType                = null;
	protected Node dbNode                     = null;

	//~--- constructors ---------------------------------------------------
	public AbstractNode() {
	}

	public AbstractNode(SecurityContext securityContext, final Node dbNode, final Class entityType) {
		init(securityContext, dbNode, entityType, false);
	}

	//~--- methods --------------------------------------------------------
	@Override
	public void onNodeCreation() {
	}

	@Override
	public void onNodeInstantiation(final boolean isCreation) {
		this.cachedUuid = getProperty(GraphObject.id);
	}

	@Override
	public void onNodeDeletion() {
	}

	@Override
	public final void init(final SecurityContext securityContext, final Node dbNode, final Class entityType, final boolean isCreation) {

		this.isCreation      = isCreation;
		this.dbNode          = dbNode;
		this.entityType      = entityType;
		this.securityContext = securityContext;
	}

	@Override
	public final void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public final SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public boolean equals(final Object o) {

		if (o == null) {

			return false;
		}

		if (!(o instanceof AbstractNode)) {

			return false;
		}

		return (Integer.valueOf(this.hashCode()).equals(o.hashCode()));

	}

	@Override
	public int hashCode() {

		if (this.dbNode == null) {

			return (super.hashCode());
		}

		return Long.valueOf(dbNode.getId()).hashCode();

	}

	@Override
	public int compareTo(final Object other) {

		if (other instanceof AbstractNode) {

			final AbstractNode node = (AbstractNode)other;
			if (node == null) {
				return -1;
			}

			String name = getName();

			if (name == null) {
				return -1;
			}

			String nodeName = node.getName();

			if (nodeName == null) {
				return -1;
			}

			return name.compareTo(nodeName);
		}

		if (other instanceof String) {

			return getUuid().compareTo((String)other);

		}

		if (other == null) {
			throw new NullPointerException();
		}

		throw new IllegalStateException("Cannot compare " + this + " to " + other);
	}

	/**
	 * Implement standard toString() method
	 */
	@Override
	public String toString() {
		return getUuid();

	}

	/**
	 * Can be used to permit the setting of a read-only property once. The
	 * lock will be restored automatically after the next setProperty
	 * operation. This method exists to prevent automatic set methods from
	 * setting a read-only property while allowing a manual set method to
	 * override this default behaviour.
	 */
	@Override
	public final void unlockReadOnlyPropertiesOnce() {
		this.readOnlyPropertiesUnlocked = true;
	}

	/**
	 * Can be used to permit the setting of a system property once. The
	 * lock will be restored automatically after the next setProperty
	 * operation. This method exists to prevent automatic set methods from
	 * setting a system property while allowing a manual set method to
	 * override this default behaviour.
	 */
	@Override
	public final void unlockSystemPropertiesOnce() {
		this.internalSystemPropertiesUnlocked = true;
		unlockReadOnlyPropertiesOnce();
	}

	@Override
	public final void removeProperty(final PropertyKey key) throws FrameworkException {

		if (!isGranted(Permission.write, securityContext)) {
			throw new FrameworkException(403, "Modification not permitted.");
		}

		if (this.dbNode != null) {

			if (key == null) {

				logger.error("Tried to set property with null key (action was denied)");

				return;

			}

			// check for read-only properties
			if (key.isReadOnly()) {

				// allow super user to set read-only properties
				if (readOnlyPropertiesUnlocked || securityContext.isSuperUser()) {

					// permit write operation once and
					// lock read-only properties again
					internalSystemPropertiesUnlocked = false;
				} else {

					throw new FrameworkException(404, "Property " + key.jsonName() + " is read-only", new ReadOnlyPropertyToken(getType(), key));
				}

			}

			// check for system properties - cannot be overriden with super-user rights
			if (key.isSystemInternal()) {

				// allow super user to set read-only properties
				if (internalSystemPropertiesUnlocked) {

					// permit write operation once and
					// lock read-only properties again
					internalSystemPropertiesUnlocked = false;
				} else {

					throw new FrameworkException(404, "Property " + key.jsonName() + " is read-only", new InternalSystemPropertyToken(getType(), key));
				}

			}

			dbNode.removeProperty(key.dbName());

			// remove from index
			removeFromIndex(key);
		}

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public final PropertyKey getDefaultSortKey() {
		return AbstractNode.name;
	}

	@Override
	public final String getDefaultSortOrder() {
		return GraphObjectComparator.ASCENDING;
	}

	@Override
	public final String getType() {
		return getProperty(AbstractNode.type);
	}

	@Override
	public final PropertyContainer getPropertyContainer() {
		return dbNode;
	}

	/**
	 * Get name from underlying db node
	 *
	 * If name is null, return node id as fallback
	 */
	@Override
	public final String getName() {

		String name = getProperty(AbstractNode.name);
		if (name == null) {

			name = getNodeId().toString();
		}

		return name;
	}

	/**
	 * Get id from underlying db
	 */
	@Override
	public final long getId() {

		if (dbNode == null) {

			return -1;
		}

		return dbNode.getId();

	}

	@Override
	public final String getUuid() {

		if (cachedUuid == null) {
			cachedUuid = getProperty(GraphObject.id);
		}

		return cachedUuid;
	}

	public final Long getNodeId() {
		return getId();

	}

	public final String getIdString() {
		return Long.toString(getId());
	}

	/**
	 * Indicates whether this node is visible to public users.
	 *
	 * @return whether this node is visible to public users
	 */
	public final boolean getVisibleToPublicUsers() {
		return getProperty(visibleToPublicUsers);
	}

	/**
	 * Indicates whether this node is visible to authenticated users.
	 *
	 * @return whether this node is visible to authenticated users
	 */
	public final boolean getVisibleToAuthenticatedUsers() {
		return getProperty(visibleToPublicUsers);
	}

	/**
	 * Indicates whether this node is hidden.
	 *
	 * @return whether this node is hidden
	 */
	public final boolean getHidden() {
		return getProperty(hidden);
	}

	/**
	 * Indicates whether this node is deleted.
	 *
	 * @return whether this node is deleted
	 */
	public final boolean getDeleted() {
		return getProperty(deleted);
	}

	/**
	 * Returns the property set for the given view as an Iterable.
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		// check for custom view in content-type field
		if (securityContext != null && securityContext.hasCustomView()) {

			final Set<PropertyKey> keys = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(entityType, propertyView));
			final Set<String> customView = securityContext.getCustomView();

			for (Iterator<PropertyKey> it = keys.iterator(); it.hasNext();) {
				if (!customView.contains(it.next().jsonName())) {

					it.remove();
				}
			}

			return keys;
		}

		// this is the default if no application/json; properties=[...] content-type header is present on the request
		return StructrApp.getConfiguration().getPropertySet(entityType, propertyView);
	}

	/**
	 * Returns the (converted, validated, transformed, etc.) property for
	 * the given property key.
	 *
	 * @param <T>
	 * @param key the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
	@Override
	public <T> T getProperty(final PropertyKey<T> key) {
		return getProperty(key, null);
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> key, final Predicate<GraphObject> predicate) {
		return getProperty(key, true, predicate);
	}

	private <T> T getProperty(final PropertyKey<T> key, boolean applyConverter, final Predicate<GraphObject> predicate) {

		// early null check, this should not happen...
		if (key == null || key.dbName() == null) {
			return null;
		}

		/**
		 * check read access:
		 * - permission resolution MUST already be done here because otherwise we won't be able to access the node
		 * - securityContext should contain the masked permissions
		 * - check property name against masked permissions
		 */
		if (permissionResolutionMask != null) {

			if (!permissionResolutionMask.allowsPermission(Permission.read)) {
				return null;
			}

			if (!permissionResolutionMask.allowsProperty(key)) {
				return null;
			}
		}

		return key.getProperty(securityContext, this, applyConverter, predicate);
	}

	public final String getPropertyMD5(final PropertyKey key) {

		Object value = getProperty(key);

		if (value instanceof String) {

			return DigestUtils.md5Hex((String) value);
		} else if (value instanceof byte[]) {

			return DigestUtils.md5Hex((byte[]) value);
		}

		logger.warn("Could not create MD5 hex out of value {}", value);

		return null;

	}

	/**
	 * Returns the property value for the given key as a Comparable
	 *
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Comparable
	 */
	@Override
	public final <T> Comparable getComparableProperty(final PropertyKey<T> key) {

		if (key != null) {

			final T propertyValue = getProperty(key);

			// check property converter
			PropertyConverter<T, ?> converter = key.databaseConverter(securityContext, this);
			if (converter != null) {

				try {
					return converter.convertForSorting(propertyValue);

				} catch (Throwable t) {

					logger.warn("Unable to convert property {} of type {}: {}", new Object[]{
						key.dbName(),
						getClass().getSimpleName(),
						t.getMessage()
					});

					logger.warn("", t);
				}
			}

			// conversion failed, may the property value itself is comparable
			if (propertyValue instanceof Comparable) {
				return (Comparable) propertyValue;
			}

			// last try: convertFromInput to String to make comparable
			if (propertyValue != null) {
				return propertyValue.toString();
			}
		}

		return null;
	}

	/**
	 * Returns the property value for the given key as a Iterable
	 *
	 * @param propertyKey the property key to retrieve the value for
	 * @return the property value for the given key as a Iterable
	 */
	public final Iterable getIterableProperty(final PropertyKey<? extends Iterable> propertyKey) {
		return (Iterable) getProperty(propertyKey);
	}

	/**
	 * Returns a list of related nodes for which a modification propagation
	 * is configured via the relationship. Override this method to return a
	 * set of nodes that should receive propagated modifications.
	 *
	 * @return a set of nodes to which modifications should be propagated
	 */
	public Set<AbstractNode> getNodesForModificationPropagation() {
		return null;
	}

	/**
	 * Returns database node.
	 *
	 * @return the database node
	 */
	@Override
	public final Node getNode() {

		return dbNode;

	}

	protected final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationshipsAsSuperUser(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(SecurityContext.getSuperUserInstance());
		final R template = getRelationshipForType(type);

		return new IterableAdapter<>(template.getSource().getRawSource(SecurityContext.getSuperUserInstance(), dbNode, null), factory);
	}

	@Override
	public final <R extends AbstractRelationship> Iterable<R> getRelationships() {
		return new IterableAdapter<>(dbNode.getRelationships(), new RelationshipFactory<R>(securityContext));
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template = getRelationshipForType(type);
		final Direction direction = template.getDirectionForType(entityType);
		final RelationshipType relType = template;

		return new IterableAdapter<>(dbNode.getRelationships(direction, relType), factory);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template = getRelationshipForType(type);
		final Relationship relationship = template.getSource().getRawSource(securityContext, dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template = getRelationshipForType(type);

		return new IterableAdapter<>(new IdSorter<>(template.getSource().getRawSource(securityContext, dbNode, null)), factory);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template = getRelationshipForType(type);
		final Relationship relationship = template.getTarget().getRawSource(securityContext, dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	protected final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> R getOutgoingRelationshipAsSuperUser(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(SecurityContext.getSuperUserInstance());
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getSource().getRawTarget(SecurityContext.getSuperUserInstance(), dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template = getRelationshipForType(type);

		return new IterableAdapter<>(new IdSorter<>(template.getTarget().getRawSource(securityContext, dbNode, null)), factory);
	}

	@Override
	public final <R extends AbstractRelationship> Iterable<R> getIncomingRelationships() {
		return new IterableAdapter<>(new IdSorter<>(dbNode.getRelationships(Direction.INCOMING)), new RelationshipFactory<R>(securityContext));
	}

	@Override
	public final <R extends AbstractRelationship> Iterable<R> getOutgoingRelationships() {
		return new IterableAdapter<>(new IdSorter<>(dbNode.getRelationships(Direction.OUTGOING)), new RelationshipFactory<R>(securityContext));
	}

	@Override
	public final <R extends AbstractRelationship> Iterable<R> getRelationshipsAsSuperUser() {
		return new IterableAdapter<>(dbNode.getRelationships(), new RelationshipFactory<R>(SecurityContext.getSuperUserInstance()));
	}

	/**
	 * Return statistical information on all relationships of this node
	 *
	 * @param dir
	 * @return number of relationships
	 */
	public final Map<String, Long> getRelationshipInfo(final Direction dir) throws FrameworkException {
		return StructrApp.getInstance(securityContext).command(NodeRelationshipStatisticsCommand.class).execute(this, dir);
	}

	/**
	 * Returns the owner node of this node, following an INCOMING OWNS
	 * relationship.
	 *
	 * @return the owner node of this node
	 */
	@Override
	public final Principal getOwnerNode() {

		if (cachedOwnerNode == null) {

			final Ownership ownership = getIncomingRelationshipAsSuperUser(PrincipalOwnsNode.class);
			if (ownership != null) {

				Principal principal = ownership.getSourceNode();
				cachedOwnerNode = (Principal) principal;
			}
		}

		return cachedOwnerNode;
	}

	/**
	 * Returns the database ID of the owner node of this node.
	 *
	 * @return the database ID of the owner node of this node
	 */
	public final Long getOwnerId() {

		return getOwnerNode().getId();

	}

	protected <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationshipAsSuperUser(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(SecurityContext.getSuperUserInstance());
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getSource().getRawSource(SecurityContext.getSuperUserInstance(), dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	/**
	 * Return true if this node has a relationship of given type and
	 * direction.
	 *
	 * @param <A>
	 * @param <B>
	 * @param <S>
	 * @param <T>
	 * @param type
	 * @return relationships
	 */
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final Class<? extends Relation<A, B, S, T>> type) {
		return this.getRelationships(type).iterator().hasNext();
	}

	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final Class<R> type) {
		return getRelationshipForType(type).getSource().hasElements(securityContext, dbNode, null);
	}

	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Class<R> type) {
		return getRelationshipForType(type).getTarget().hasElements(securityContext, dbNode, null);
	}

	// ----- interface AccessControllable -----
	@Override
	public final boolean isGranted(final Permission permission, final SecurityContext context) {

		// super user can do everything
		if (context != null && context.isSuperUser()) {
			return true;
		}

		Principal accessingUser = null;
		if (context != null) {

			accessingUser = context.getUser(false);
		}

		return isGranted(permission, accessingUser, 0, new HashSet<>());
	}

	private boolean isGranted(final Permission permission, final Principal accessingUser, final int level, final Set<Long> alreadyTraversed) {

		if (level > 100) {
			logger.warn("Aborting recursive permission resolution because of recursion level > 100, this is quite likely an infinite loop.");
			return false;
		}

		// use quick checks for maximum performance
		if (isCreation && (accessingUser == null || accessingUser.equals(this) || accessingUser.equals(getOwnerNode()) ) ) {
			return true;
		}

		// this includes SuperUser
		if (accessingUser != null && accessingUser.isAdmin()) {
			return true;
		}

		// allow accessingUser to access itself, but not parents etc.
		if (this.equals(accessingUser) && (level == 0 || (permission.equals(Permission.read) && level > 0))) {
			return true;
		}

		// check owner
		final Principal _owner = getOwnerNode();
		final boolean hasOwner = (_owner != null);

		// allow full access for nodes without owner
		// (covered by ResourceAccess objects)
		if (!hasOwner && Services.getPermissionsForOwnerlessNodes().contains(permission)) {

			if (accessingUser != null && isVisibleToAuthenticatedUsers()) {
				return true;
			}

			if (accessingUser == null && isVisibleToPublicUsers()) {
				return true;
			}
		}

		// node has an owner, deny anonymous access
		if (hasOwner && accessingUser == null) {
			return false;
		}

		if (accessingUser != null) {

			// owner is always allowed to do anything with its nodes
			if (hasOwner && accessingUser.equals(_owner)) {
				return true;
			}

			final Security security = getSecurityRelationship(accessingUser);
			if (security != null && security.isAllowed(permission)) {
				return true;
			}

			// Check permissions from domain relationships
			if (hasEffectivePermissions(accessingUser, permission)) {
				return true;
			}

			// Last: recursively check possible parent principals
			for (Principal parent : accessingUser.getParents()) {

				if (isGranted(permission, parent, level+1, alreadyTraversed)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean hasEffectivePermissions(final Principal principal, final Permission permission) {

		final boolean doLog = securityContext.hasParameter("debugLoggingEnabled");

		// don't check relationship propagation if there are no propagating relationships
		if (SchemaRelationshipNode.getPropagatingRelationshipTypes().isEmpty()) {
			return false;
		}

		if (doLog) {
			System.out.println("\n#######################################################\nResolving " + permission.name() + " for user " + principal.getName() + " to " + this.getType() + " (" + this.getUuid() + ")");
		}

		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final RelationshipFactory relFactory   = new RelationshipFactory(superUserContext);
		PermissionResolutionMask mask          = AccessPathCache.get(principal, this);

		// current path segment has precedence over path based permission resolution mask
		if (rawPathSegment != null) {

			final boolean result = checkPathSegment(principal, permission, relFactory);

			if (doLog) {

				if (result) {

					System.out.println("        " + permission.name() + " ALLOWED by path segment " + rawPathSegment.getType());

				} else {

					System.out.println("        " + permission.name() + " DENIED by path segment " + rawPathSegment.getType());
				}
			}

			if (result) {
				return true;
			}
		}

		// use cached result only when it was already checked for the given permission
		if (mask != null && mask.alreadyChecked(permission)) {

			final boolean result = mask.allowsPermission(permission);

			if (doLog) {
				if (result) {

					System.out.println("        " + permission.name() + " ALLOWED by cached mask " + mask);

				} else {

					System.out.println("        " + permission.name() + " DENIED by cached mask " + mask);
				}
			}

			return result;
		}

		try {

			if (mask == null) {

				// store only a single mask for every node
				mask = new PermissionResolutionMask();
				AccessPathCache.put(principal, this, mask);

				if (doLog) {
					System.out.println("        Storing initial mask: " + mask);
				}
			}

			// store all check attempts in the cache
			mask.setChecked(permission);

			final DatabaseService db         = StructrApp.getInstance().getDatabaseService();
			final String relTypes            = getPermissionPropagationRelTypes();
			final Map<String, Object> params = new HashMap<>();
			final long principalId           = principal.getId();

			params.put("id1", principalId);
			params.put("id2", this.getId());

			// FIXME: make fixed path length of 8 configurable
			for (int i=1; i<10; i++) {

				final String query        = "MATCH (n), (m), p = allShortestPaths((n)-[" + relTypes + "*.." + i + "]-(m)) WHERE id(n) = {id1} AND id(m) = {id2} RETURN p";
				final NativeResult result = db.execute(query, params);

				while (result.hasNext()) {

					final Map<String, Object> row = result.next();
					final Path path               = (Path)row.get("p");
					Node previousNode             = null;
					boolean arrived               = true;

					for (final PropertyContainer container : path) {

						if (container instanceof Node) {

							// store previous node to determine relationship direction
							previousNode = (Node)container;
							AccessPathCache.update(principal, this, previousNode);

						} else {

							final Relationship rel        = (Relationship)container;
							final RelationshipInterface r = relFactory.instantiate(rel);

							if (r instanceof PermissionPropagation) {

								// update cache with relationship type
								AccessPathCache.update(principal, this, rel);

								final PermissionPropagation propagation                     = (PermissionPropagation)r;
								final long startNodeId                                      = rel.getStartNode().getId();
								final long thisId                                           = previousNode.getId();
								final SchemaRelationshipNode.Direction relDirection         = thisId == startNodeId ? SchemaRelationshipNode.Direction.Out : SchemaRelationshipNode.Direction.In;
								final SchemaRelationshipNode.Direction propagationDirection = propagation.getPropagationDirection();

								// check propagation direction
								if (!propagationDirection.equals(SchemaRelationshipNode.Direction.Both)) {

									if (propagationDirection.equals(SchemaRelationshipNode.Direction.None)) {

										mask.clear();
										arrived = false;
										break;
									}

									if (!relDirection.equals(propagationDirection)) {

										mask.clear();
										arrived = false;
										break;
									}
								}

								applyCurrentStep(propagation, mask);

								// break early
								if (!mask.allowsPermission(permission)) {

									if (doLog) {
										System.out.println("        " + permission.name() + " DENIED by " + path);
									}

									arrived = false;
									break;
								}

							} else {

								if (doLog) {
									System.out.println("        " + permission.name() + " DENIED by " + path);
								}

								arrived = false;
								break;
							}
						}
					}


					if (arrived && mask.allowsPermission(permission)) {

						if (doLog) {
							System.out.println("        " + permission.name() + " ALLOWED by " + path);
							System.out.println("        Storing mask from path: " + mask);
						}

						AccessPathCache.put(principal, this, mask);

						return true;
					}
				}
			}

		} catch (Throwable t) {
			logger.warn("", t);
		}

		mask.setPermission(permission, false);
		AccessPathCache.put(principal, this, mask);

		if (doLog) {
			System.out.println("        Storing mask from unsuccessful path: " + mask);
		}

		return false;
	}

	private boolean checkPathSegment(final Principal principal, final Permission permission, final RelationshipFactory relFactory) {

		final boolean doLog = securityContext.hasParameter("debugLoggingEnabled");
		final RelationshipInterface r = relFactory.instantiate(rawPathSegment);
		if (r instanceof PermissionPropagation) {

			final PermissionPropagation propagation                     = (PermissionPropagation)r;
			final long startNodeId                                      = rawPathSegment.getStartNode().getId();
			final long thisId                                           = getId();
			final SchemaRelationshipNode.Direction relDirection         = thisId == startNodeId ? SchemaRelationshipNode.Direction.In : SchemaRelationshipNode.Direction.Out;
			final SchemaRelationshipNode.Direction propagationDirection = propagation.getPropagationDirection();
			final PermissionResolutionMask mask                         = new PermissionResolutionMask();

			// check propagation direction
			if (!propagationDirection.equals(SchemaRelationshipNode.Direction.Both)) {

				if (propagationDirection.equals(SchemaRelationshipNode.Direction.None)) {
					return false;
				}

				if (!relDirection.equals(propagationDirection)) {
					return false;
				}
			}

			// we can safely assume here that we arrived at this node with
			// the read permission, because otherwise the node would not
			// have been visible.
			mask.setPermission(Permission.read, true);

			// apply current
			applyCurrentStep(propagation, mask);

			if (mask.allowsPermission(permission)) {

				mask.setChecked(permission);
				AccessPathCache.put(principal, this, mask);

				if (doLog) {
					System.out.println("Storing mask from path segment: " + mask);
				}

				return true;
			}
		}

		return false;
	}

	private void applyCurrentStep(final PermissionPropagation rel, PermissionResolutionMask mask) {

		final boolean doLog = securityContext.hasParameter("debugLoggingEnabled");

		switch (rel.getReadPropagation()) {
			case Add:
				mask.addRead();
				if (doLog) { System.out.println("                add read"); }
				break;

			case Remove:
				mask.removeRead();
				if (doLog) { System.out.println("                remove read"); }
				break;

			default: break;
		}

		switch (rel.getWritePropagation()) {
			case Add:
				mask.addWrite();
				if (doLog) { System.out.println("                add write"); }
				break;

			case Remove:
				mask.removeWrite();
				if (doLog) { System.out.println("                remove write"); }
				break;

			default: break;
		}

		switch (rel.getDeletePropagation()) {
			case Add:
				mask.addDelete();
				if (doLog) { System.out.println("                add delete"); }
				break;

			case Remove:
				mask.removeDelete();
				if (doLog) { System.out.println("                remove delete"); }
				break;

			default: break;
		}

		switch (rel.getAccessControlPropagation()) {
			case Add:
				mask.addAccessControl();
				if (doLog) { System.out.println("                add accessControl"); }
				break;

			case Remove:
				mask.removeAccessControl();
				if (doLog) { System.out.println("                remove accessControl"); }
				break;

			default: break;
		}

		// handle delta properties
		mask.handleProperties(rel.getDeltaProperties());
	}

	/**
	 * Return the (cached) incoming relationship between this node and the
	 * given principal which holds the security information.
	 *
	 * @param p
	 * @return incoming security relationship
	 */
	@Override
	public final Security getSecurityRelationship(final Principal p) {

		if (p == null) {

			return null;
		}

		for (final Security r : getIncomingRelationshipsAsSuperUser(Security.class)) {

			if (r != null) {

				if (p.equals(r.getSourceNode())) {

					return r;

				}
			}
		}

		return null;

	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		return true;
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

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = true;

		valid &= ValidationHelper.isValidStringNotBlank(this, id, errorBuffer);
		valid &= ValidationHelper.isValidGloballyUniqueProperty(this, id, errorBuffer);
		valid &= ValidationHelper.isValidStringMatchingRegex(this, id, "[a-fA-F0-9]{32}", errorBuffer);
		valid &= ValidationHelper.isValidStringNotBlank(this, type, errorBuffer);

		return valid;

	}

	@Override
	public final boolean isVisibleToPublicUsers() {

		return getVisibleToPublicUsers();

	}

	@Override
	public final boolean isVisibleToAuthenticatedUsers() {

		return getProperty(visibleToAuthenticatedUsers);

	}

	@Override
	public final boolean isNotHidden() {

		return !getHidden();

	}

	@Override
	public final boolean isHidden() {

		return getHidden();

	}

	@Override
	public final Date getVisibilityStartDate() {
		return getProperty(visibilityStartDate);
	}

	@Override
	public final Date getVisibilityEndDate() {
		return getProperty(visibilityEndDate);
	}

	@Override
	public final Date getCreatedDate() {
		return getProperty(createdDate);
	}

	@Override
	public final Date getLastModifiedDate() {
		return getProperty(lastModifiedDate);
	}

	// ----- end interface AccessControllable -----
	public final boolean isNotDeleted() {

		return !getDeleted();

	}

	@Override
	public final boolean isDeleted() {

		return getDeleted();

	}

	/**
	 * Return true if node is the root node
	 *
	 * @return isRootNode
	 */
	public final boolean isRootNode() {

		return getId() == 0;

	}

	public final boolean isVisible() {

		return securityContext.isVisible(this);

	}

	/**
	 * Set a property in database backend. This method needs to be wrappend
	 * into a StructrTransaction, otherwise Neo4j will throw a
	 * NotInTransactionException! Set property only if value has changed.
	 *
	 * @param <T>
	 * @param key
	 * @throws org.structr.common.error.FrameworkException
	 */
	@Override
	public <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {

		// allow setting of ID without permissions
		if (!key.equals(GraphObject.id)) {

			if (!isGranted(Permission.write, securityContext)) {

				internalSystemPropertiesUnlocked = false;
				readOnlyPropertiesUnlocked       = false;

				throw new FrameworkException(403, "Modification not permitted.");
			}
		}

		T oldValue = getProperty(key);

		// no old value exists  OR  old value exists and is NOT equal => set property
		if ( ((oldValue == null) && (value != null)) || ((oldValue != null) && (!oldValue.equals(value)) || (key instanceof FunctionProperty)) ) {

			return setPropertyInternal(key, value);

		}

		internalSystemPropertiesUnlocked = false;
		readOnlyPropertiesUnlocked       = false;

		return null;
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {

		if (!isGranted(Permission.write, securityContext)) {

			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;

			throw new FrameworkException(403, "Modification not permitted.");
		}

		for (final PropertyKey key : properties.keySet()) {

			if (dbNode != null && dbNode.hasProperty(key.dbName())) {

				// check for system properties
				if (key.isSystemInternal() && !internalSystemPropertiesUnlocked) {

					throw new FrameworkException(422, "Property " + key.jsonName() + " is an internal system property", new InternalSystemPropertyToken(getClass().getSimpleName(), key));

				}

				// check for read-only properties
				if ((key.isReadOnly() || key.isWriteOnce()) && !readOnlyPropertiesUnlocked && !securityContext.isSuperUser()) {

					throw new FrameworkException(422, "Property " + key.jsonName() + " is read-only", new ReadOnlyPropertyToken(getClass().getSimpleName(), key));

				}

			}
		}

		NodeInterface.super.setProperties(securityContext, properties);
	}

	private <T> Object setPropertyInternal(final PropertyKey<T> key, final T value) throws FrameworkException {

		if (key == null) {

			logger.error("Tried to set property with null key (action was denied)");

			throw new FrameworkException(422, "Tried to set property with null key (action was denied)", new NullArgumentToken(getClass().getSimpleName(), base));

		}

		try {
			if (dbNode != null && dbNode.hasProperty(key.dbName())) {

				// check for system properties
				if (key.isSystemInternal() && !internalSystemPropertiesUnlocked) {

					throw new FrameworkException(422, "Property " + key.jsonName() + " is an internal system property", new InternalSystemPropertyToken(getClass().getSimpleName(), key));

				}

				// check for read-only properties
				if ((key.isReadOnly() || key.isWriteOnce()) && !readOnlyPropertiesUnlocked && !securityContext.isSuperUser()) {

					throw new FrameworkException(422, "Property " + key.jsonName() + " is read-only", new ReadOnlyPropertyToken(getClass().getSimpleName(), key));

				}

			}

			return key.setProperty(securityContext, this, value);

		} finally {

			// unconditionally lock read-only properties after every write (attempt) to avoid security problems
			// since we made "unlock_readonly_properties_once" available through scripting
			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;
		}

	}

	@Override
	public final void addToIndex() {

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(entityType, PropertyView.All)) {

			if (key.isIndexed()) {

				key.index(this, this.getProperty(key));
			}
		}
	}


	@Override
	public final void updateInIndex() {

		removeFromIndex();
		addToIndex();
	}

	@Override
	public final void removeFromIndex() {

		final Index<Node> index = Services.getInstance().getService(NodeService.class).getNodeIndex();
		index.remove(dbNode);
	}

	public final void removeFromIndex(PropertyKey key) {

		final Index<Node> index = Services.getInstance().getService(NodeService.class).getNodeIndex();
		index.remove(dbNode, key.dbName());
	}

	@Override
	public final void indexPassiveProperties() {

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(entityType, PropertyView.All)) {

			if (key.isPassivelyIndexed()) {

				key.index(this, this.getProperty(key));
			}
		}
	}

	public static void clearRelationshipTemplateInstanceCache() {
		relationshipTemplateInstanceCache.clear();
	}

	public static <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R getRelationshipForType(final Class<R> type) {

		R instance = (R) relationshipTemplateInstanceCache.get(type.getName());
		if (instance == null) {

			try {

				instance = type.newInstance();
				relationshipTemplateInstanceCache.put(type.getName(), instance);

			} catch (Throwable t) {

				// TODO: throw meaningful exception here,
				// should be a RuntimeException that indicates
				// wrong use of Relationships etc.
				logger.warn("", t);
			}
		}

		return instance;
	}

	@Override
	public final String getPropertyWithVariableReplacement(ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {

		final Object value = getProperty(key);
		String result      = null;

		try {

			result = Scripting.replaceVariables(renderContext, this, value);

		} catch (Throwable t) {
			logger.warn("Scripting error in {}: {}:\n{}\n{}", new Object[] { key.dbName(), getUuid(), t, value });
			t.printStackTrace();
		}

		return result;
	}

	@Override
	public final Object evaluate(final SecurityContext securityContext, final String key, final String defaultValue) throws FrameworkException {

		switch (key) {

			case "owner":
				return getOwnerNode();

			case "_path":
				if (rawPathSegment != null) {

					return new RelationshipFactory<>(securityContext).adapt(rawPathSegment);

				} else {

					return null;
				}

			default:

				// evaluate object value or return default
				Object value = getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, key));

				if (value != null) {
					return value;
				}

				value = invokeMethod(key, Collections.EMPTY_MAP, false);
				if (value != null) {
					return value;
				}

				return Function.numberOrString(defaultValue);
		}
	}

	@Override
	public final Object invokeMethod(final String methodName, final Map<String, Object> propertySet, final boolean throwExceptionForUnknownMethods) throws FrameworkException {

		final Method method = StructrApp.getConfiguration().getExportedMethodsForType(entityType).get(methodName);
		if (method != null) {

			try {

				// First, try if single parameter is a map, then directly invoke method
				if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(Map.class)) {
					return method.invoke(this, propertySet);
				}

				// second try: extracted parameter list
				return method.invoke(this, extractParameters(propertySet, method.getParameterTypes()));

			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException t) {

				if (t instanceof FrameworkException) {

					throw (FrameworkException) t;

				} else if (t.getCause() instanceof FrameworkException) {

					throw (FrameworkException) t.getCause();

				} else {

					logger.debug("Unable to invoke method {}: {}", new Object[]{methodName, t.getMessage()});
					logger.warn("", t);

				}
			}
		}

		// in the case of REST access we want to know if the method exists or not
		if (throwExceptionForUnknownMethods) {
			throw new FrameworkException(400, "Method " + methodName + " not found in type " + getType());
		}

		return null;
	}

	private Object[] extractParameters(Map<String, Object> properties, Class[] parameterTypes) {

		final List<Object> values = new ArrayList<>(properties.values());
		final List<Object> parameters = new ArrayList<>();
		int index = 0;

		// only try to convert when both lists have equal size
		if (values.size() == parameterTypes.length) {

			for (final Class parameterType : parameterTypes) {

				final Object value = convert(values.get(index++), parameterType);
				if (value != null) {

					parameters.add(value);
				}
			}
		}

		return parameters.toArray(new Object[0]);
	}

	/*
	 * Tries to convert the given value into an object
	 * of the given type, using an intermediate type
	 * of String for the conversion.
	 */
	private Object convert(Object value, Class type) {

		Object convertedObject = null;

		if (type.equals(String.class)) {

			// strings can be returned immediately
			return value.toString();

		} else if (value instanceof Number) {

			Number number = (Number) value;

			if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
				return number.intValue();

			} else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
				return number.longValue();

			} else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
				return number.doubleValue();

			} else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
				return number.floatValue();

			} else if (type.equals(Short.class) || type.equals(Integer.TYPE)) {
				return number.shortValue();

			} else if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
				return number.byteValue();

			}

		} else if (value instanceof List) {

			return value;

		} else if (value instanceof Map) {
			return value;
		}

		// fallback
		try {

			Method valueOf = type.getMethod("valueOf", String.class);
			if (valueOf != null) {

				convertedObject = valueOf.invoke(null, value.toString());

			} else {

				logger.warn("Unable to find static valueOf method for type {}", type);
			}

		} catch (Throwable t) {

			logger.warn("Unable to deserialize value {} of type {}, Class has no static valueOf method.", new Object[]{value, type});
		}

		return convertedObject;
	}

	@Override
	public final void grant(Permission permission, Principal principal) throws FrameworkException {

		if (!isGranted(Permission.accessControl, securityContext)) {
			throw new FrameworkException(403, "Access control not permitted");
		}

		Security secRel = getSecurityRelationship(principal);
		if (secRel == null) {

			try {

				secRel = StructrApp.getInstance().create(principal, (NodeInterface)this, Security.class);

			} catch (FrameworkException ex) {

				logger.error("Could not create security relationship!", ex);

			}

		}

		secRel.addPermission(permission);

	}

	@Override
	public final void revoke(Permission permission, Principal principal) throws FrameworkException {

		if (!isGranted(Permission.accessControl, securityContext)) {
			throw new FrameworkException(403, "Access control not permitted");
		}

		Security secRel = getSecurityRelationship(principal);
		if (secRel == null) {

			logger.error("Could not create revoke permission, no security relationship exists!");

		} else {

			secRel.removePermission(permission);
		}
	}

	@Override
	public final void setRawPathSegment(final Relationship rawPathSegment) {
		this.rawPathSegment = rawPathSegment;
	}

	@Override
	public final Relationship getRawPathSegment() {
		return rawPathSegment;
	}

	public final void revokeAll() throws FrameworkException {

		if (!isGranted(Permission.accessControl, securityContext)) {
			throw new FrameworkException(403, "Access control not permitted");
		}

		final App app = StructrApp.getInstance();

		for (final Security security : getIncomingRelationshipsAsSuperUser(Security.class)) {
			app.delete(security);
		}
	}

	@Override
	public final PermissionResolutionMask getPermissionResolutionMask() {
		return permissionResolutionMask;
	}

	public List<Security> getSecurityRelationships() {

		final List<Security> grants = Iterables.toList(getIncomingRelationshipsAsSuperUser(Security.class));

		// sort list by principal name (important for diff'able export)
		Collections.sort(grants, new Comparator<Security>() {

			@Override
			public int compare(final Security o1, final Security o2) {

				final Principal p1 = o1.getSourceNode();
				final Principal p2 = o2.getSourceNode();
				final String n1    = p1 != null ? p1.getProperty(AbstractNode.name) : "empty";
				final String n2    = p2 != null ? p2.getProperty(AbstractNode.name) : "empty";

				if (n1 != null && n2 != null) {
					return n1.compareTo(n2);

				} else if (n1 != null) {

					return 1;

				} else if (n2 != null) {
					return -1;
				}

				return 0;
			}
		});

		return grants;
	}

	private String getPermissionPropagationRelTypes() {
		return ":" + StringUtils.join(SchemaRelationshipNode.getPropagatingRelationshipTypes(), "|");
	}

	// ----- Cloud synchronization and replication -----
	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		return new ArrayList<>(); // provide a basis for super.getSyncData() calls
	}

	@Override
	public final boolean isNode() {
		return true;
	}

	@Override
	public final boolean isRelationship() {
		return false;
	}

	@Override
	public final NodeInterface getSyncNode() {
		return this;
	}

	@Override
	public final RelationshipInterface getSyncRelationship() {
		throw new ClassCastException(this.getClass() + " cannot be cast to org.structr.core.graph.RelationshipInterface");
	}

	// ----- CMIS support methods -----
	@Override
	public CMISInfo getCMISInfo() {
		return this;
	}

	@Override
	public BaseTypeId getBaseTypeId() {
		return BaseTypeId.CMIS_ITEM;
	}

	@Override
	public CMISFolderInfo getFolderInfo() {
		return null;
	}

	@Override
	public CMISDocumentInfo getDocumentInfo() {
		return null;
	}

	@Override
	public CMISItemInfo geItemInfo() {
		return this;
	}

	@Override
	public CMISRelationshipInfo getRelationshipInfo() {
		return null;
	}

	@Override
	public CMISPolicyInfo getPolicyInfo() {
		return null;
	}

	@Override
	public CMISSecondaryInfo getSecondaryInfo() {
		return null;
	}

	@Override
	public String getCreatedBy() {
		return getProperty(AbstractNode.createdBy);
	}

	@Override
	public String getLastModifiedBy() {
		return getProperty(AbstractNode.lastModifiedBy);
	}

	@Override
	public GregorianCalendar getLastModificationDate() {

		final Date creationDate = getProperty(AbstractNode.lastModifiedDate);
		if (creationDate != null) {

			final GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(creationDate);

			return calendar;
		}

		return null;
	}

	@Override
	public GregorianCalendar getCreationDate() {

		final Date creationDate = getProperty(AbstractNode.createdDate);
		if (creationDate != null) {

			final GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(creationDate);

			return calendar;
		}

		return null;
	}

	@Override
	public PropertyMap getDynamicProperties() {

		final PropertyMap propertyMap       = new PropertyMap();
		final Class type                    = getClass();

		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(type, PropertyView.All)) {

			// include all dynamic keys in definition
			if (key.isDynamic() || key.isCMISProperty()) {

				// only include primitives here
				final PropertyType dataType = key.getDataType();
				if (dataType != null) {

					propertyMap.put(key, getProperty(key));
				}
			}
		}



		return propertyMap;
	}

	@Override
	public AllowableActions getAllowableActions() {
		return new StructrItemActions();
	}

	@Override
	public List<Ace> getAccessControlEntries() {

		final List<Ace> entries = new LinkedList<>();

		for (final Security security : getIncomingRelationshipsAsSuperUser(Security.class)) {

			if (security != null) {

				entries.add(new AceEntry(security));
			}
		}

		return entries;
	}

	// ----- nested classes -----
	private static class AceEntry extends CMISExtensionsData implements Ace, org.apache.chemistry.opencmis.commons.data.Principal {

		private final List<String> permissions = new LinkedList<>();
		private String principalId             = null;

		/**
		 * Construct a new AceEntry from the given Security relationship. This
		 * method assumes that is is called in a transaction.
		 *
		 * @param security
		 */
		public AceEntry(final Security security) {

			final Principal principal = security.getSourceNode();
			if (principal != null) {

				this.principalId = principal.getProperty(Principal.name);
			}

			permissions.addAll(security.getPermissions());
		}

		@Override
		public org.apache.chemistry.opencmis.commons.data.Principal getPrincipal() {
			return this;
		}

		@Override
		public String getPrincipalId() {
			return principalId;
		}

		@Override
		public List<String> getPermissions() {
			return permissions;
		}

		@Override
		public boolean isDirect() {
			return true;
		}

		// ----- interface Principal -----
		@Override
		public String getId() {
			return getPrincipalId();
		}
	}
}
