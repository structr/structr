/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.graph.*;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.common.*;
import org.structr.common.error.*;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.relationship.Ownership;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.*;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Abstract base class for all node entities in Structr.
 */
public abstract class AbstractNode implements NodeInterface, AccessControllable {

	private static final int permissionResolutionMaxLevel                                                     = Settings.ResolutionDepth.getValue();
	private static final Logger logger                                                                        = LoggerFactory.getLogger(AbstractNode.class.getName());
	private static final FixedSizeCache<String, Boolean> isGrantedResultCache                                 = new FixedSizeCache<String, Boolean>("Grant result cache", 100000);
	private static final FixedSizeCache<String, Object> relationshipTemplateInstanceCache                     = new FixedSizeCache<>("Relationship template cache", 1000);
	private static final Map<String, Map<String, PermissionResolutionResult>> globalPermissionResolutionCache = new HashMap<>();

	public static final View defaultView = new View(AbstractNode.class, PropertyView.Public, id, type, name);

	public static final View uiView = new View(AbstractNode.class, PropertyView.Ui,
		id, name, owner, type, createdBy, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers
	);

	private Map<String, Object> tmpStorageContainer = null;
	public boolean internalSystemPropertiesUnlocked = false;
	private Identity rawPathSegmentId               = null;
	private long sourceTransactionId                = -1;
	private boolean readOnlyPropertiesUnlocked      = false;
	protected String cachedUuid                     = null;
	protected SecurityContext securityContext       = null;
	protected Principal cachedOwnerNode             = null;
	protected Class entityType                      = null;
	protected Node dbNode                           = null;

	public AbstractNode() {
	}

	public AbstractNode(SecurityContext securityContext, final Node dbNode, final Class entityType, final long sourceTransactionId) {
		init(securityContext, dbNode, entityType, sourceTransactionId);
	}

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
	public final void init(final SecurityContext securityContext, final Node dbNode, final Class entityType, final long sourceTransactionId) {

		this.sourceTransactionId = sourceTransactionId;
		this.dbNode              = dbNode;
		this.entityType          = entityType;
		this.securityContext     = securityContext;

		// simple validity check
		if (dbNode != null && !this.isGenericNode()) {

			final String typeName  = getClass().getSimpleName();
			final Object typeValue = dbNode.getProperty("type");

			if (!typeName.equals(typeValue)) {

				logger.error("{} {} failed validity check: actual type in node with ID {}: {}", typeName, getUuid(), dbNode.getId(), typeValue);
			}
		}
	}

	@Override
	public long getSourceTransactionId() {
		return sourceTransactionId;
	}

	@Override
	public Class getEntityType() {
		return entityType;
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

		return dbNode.getId().hashCode();

	}

	@Override
	public int compareTo(final Object other) {

		if (other instanceof AbstractNode) {

			final AbstractNode node = (AbstractNode)other;
			final String _name      = getName();

			if (_name == null) {
				return -1;
			}

			final String nodeName = node.getName();
			if (nodeName == null) {

				return -1;
			}

			return _name.compareTo(nodeName);
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

			final Principal currentUser = securityContext.getUser(false);
			String user = null;

			if (currentUser == null) {
				user = securityContext.isSuperUser() ? "superuser" : "anonymous";
			} else {
				user = currentUser.getProperty(AbstractNode.id);
			}

			throw new FrameworkException(403, "Modification of node " + this.getProperty(AbstractNode.id) + " with type " + this.getProperty(AbstractNode.type) + " by user " + user + " not permitted.");
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
		}

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

			name = getUuid();
		}

		return name;
	}

	@Override
	public final String getUuid() {

		if (cachedUuid == null) {
			cachedUuid = getProperty(GraphObject.id);
		}

		return cachedUuid;
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
	 * Returns the property set for the given view as an Iterable.
	 *
	 * If a custom view is set via header, this can only include properties that are also included in the current view!
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {

		// check for custom view in content-type field
		if (securityContext != null && securityContext.hasCustomView()) {

			final String view            = securityContext.isSuperUser() ? PropertyView.All : propertyView;
			final Set<PropertyKey> keys  = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(entityType, view));
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
	 * Returns database node.
	 *
	 * @return the database node
	 */
	@Override
	public final Node getNode() {
		return dbNode;
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {

		if (dbNode != null && type != null && targetNode != null) {
			return dbNode.hasRelationshipTo(type, targetNode.getNode());
		}

		return false;
	}

	@Override
	public <R extends AbstractRelationship> R getRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {

		if (dbNode != null && type != null && targetNode != null) {

			final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
			final Relationship rel               = dbNode.getRelationshipTo(type, targetNode.getNode());

			if (rel != null) {

				return factory.adapt(rel);
			}
		}

		return null;
	}

	@Override
	public final <R extends AbstractRelationship> Iterable<R> getRelationships() {
		return new IterableAdapter<>(dbNode.getRelationships(), new RelationshipFactory<R>(securityContext));
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);
		final Direction direction            = template.getDirectionForType(entityType);
		final RelationshipType relType       = template;

		return new IterableAdapter<>(dbNode.getRelationships(direction, relType), factory);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getSource().getRawSource(securityContext, dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationshipAsSuperUser(final Class<R> type) {

		final SecurityContext suContext      = SecurityContext.getSuperUserInstance();
		final RelationshipFactory<R> factory = new RelationshipFactory<>(suContext);
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getSource().getRawSource(suContext, dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);

		//return new IterableAdapter<>(new IdSorter<>(template.getSource().getRawSource(securityContext, dbNode, null)), factory);
		return new IterableAdapter<>(template.getSource().getRawSource(securityContext, dbNode, null), factory);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getTarget().getRawSource(securityContext, dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);

		return new IterableAdapter<>(template.getTarget().getRawSource(securityContext, dbNode, null), factory);
	}

	@Override
	public final <R extends AbstractRelationship> Iterable<R> getIncomingRelationships() {
		return new IterableAdapter<>(dbNode.getRelationships(Direction.INCOMING), new RelationshipFactory<>(securityContext));
	}

	@Override
	public final <R extends AbstractRelationship> Iterable<R> getOutgoingRelationships() {
		return new IterableAdapter<>(dbNode.getRelationships(Direction.OUTGOING), new RelationshipFactory<>(securityContext));
	}

	@Override
	public final <R extends AbstractRelationship> Iterable<R> getRelationshipsAsSuperUser() {
		return new IterableAdapter<>(dbNode.getRelationships(), new RelationshipFactory<>(SecurityContext.getSuperUserInstance()));
	}

	protected final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationshipsAsSuperUser(final Class<R> type) {
		return getIncomingRelationshipsAsSuperUser(type, null);
	}

	protected final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationshipsAsSuperUser(final Class<R> type, final Predicate<GraphObject> predicate) {

		final SecurityContext suContext      = SecurityContext.getSuperUserInstance();
		final RelationshipFactory<R> factory = new RelationshipFactory<>(suContext);
		final R template                     = getRelationshipForType(type);

		return new IterableAdapter<>(template.getSource().getRawSource(suContext, dbNode, predicate), factory);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationshipAsSuperUser(final Class<R> type) {

		final SecurityContext suContext      = SecurityContext.getSuperUserInstance();
		final RelationshipFactory<R> factory = new RelationshipFactory<>(suContext);
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getTarget().getRawSource(suContext, dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	protected final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationshipsAsSuperUser(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(SecurityContext.getSuperUserInstance());
		final R template                     = getRelationshipForType(type);
		final Direction direction            = template.getDirectionForType(entityType);
		final RelationshipType relType       = template;

		return new IterableAdapter<>(dbNode.getRelationships(direction, relType), factory);
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
	public final String getOwnerId() {

		return getOwnerNode().getUuid();

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

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final Class<R> type) {
		return getRelationshipForType(type).getSource().hasElements(securityContext, dbNode, null);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Class<R> type) {
		return getRelationshipForType(type).getTarget().hasElements(securityContext, dbNode, null);
	}

	// ----- interface AccessControllable -----
	@Override
	public boolean isGranted(final Permission permission, final SecurityContext context) {
		return isGranted(permission, context, false);
	}

	private boolean isGranted(final Permission permission, final SecurityContext context, final boolean isCreation) {

		// super user can do everything
		if (context != null && context.isSuperUser()) {
			return true;
		}

		Principal accessingUser = null;
		if (context != null) {

			accessingUser = context.getUser(false);
		}

		final String cacheKey = getUuid() + "." + permission.name() + "." + context.getCachedUserId();
		final Boolean cached  = isGrantedResultCache.get(cacheKey);

		if (cached != null && cached == true) {
			return true;
		}

		final boolean doLog  = securityContext.hasParameter("logPermissionResolution");
		final boolean result = isGranted(permission, accessingUser, new PermissionResolutionMask(), 0, new AlreadyTraversed(), true, doLog, isCreation);

		isGrantedResultCache.put(cacheKey, result);

		return result;
	}

	private boolean isGranted(final Permission permission, final Principal accessingUser, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final boolean resolvePermissions, final boolean doLog, final boolean isCreation) {
		return isGranted(permission, accessingUser, mask, level, alreadyTraversed, resolvePermissions, doLog, null, isCreation);
	}

	private boolean isGranted(final Permission permission, final Principal accessingUser, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final boolean resolvePermissions, final boolean doLog, final Map<String, Security> incomingSecurityRelationships, final boolean isCreation) {

		if (level > 300) {
			logger.warn("Aborting recursive permission resolution for {} on {} because of recursion level > 300, this is quite likely an infinite loop.", permission.name(), getType() + "(" + getUuid() + ")");
			return false;
		}

		if (doLog) { logger.info("{}{} ({}): {} check on level {} for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, accessingUser != null ? accessingUser.getName() : null); }

		// use quick checks for maximum performance
		if (isCreation && (accessingUser == null || accessingUser.equals(this) || accessingUser.equals(getOwnerNode()) ) ) {

			return true;
		}

		if (accessingUser != null) {

			// this includes SuperUser
			if (accessingUser.isAdmin()) {
				return true;
			}

			// schema- (type-) based permissions
			if (allowedBySchema(accessingUser, permission)) {

				if (doLog) { logger.info("{}{} ({}): {} allowed on level {} by schema configuration for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, accessingUser != null ? accessingUser.getName() : null); }
				return true;
			}
		}

		// allow accessingUser to access itself, but not parents etc.
		if (this.equals(accessingUser) && (level == 0 || (permission.equals(Permission.read) && level > 0))) {
			return true;
		}

		// check owner
		final Principal _owner = getOwnerNode();
		final boolean hasOwner = (_owner != null);

		// allow access for nodes without owner, but only on first level!
		// (covered by ResourceAccess objects)
		if (level == 0 && !hasOwner && Services.getPermissionsForOwnerlessNodes().contains(permission)) {

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

			final Map<String, Security> localIncomingSecurityRelationships = (Map<String, Security>) incomingSecurityRelationships != null ? incomingSecurityRelationships : mapSecurityRelationshipsMapped(getIncomingRelationshipsAsSuperUser(Security.class));
			final Security security                                        = getSecurityRelationship(accessingUser, localIncomingSecurityRelationships);

			if (security != null && security.isAllowed(permission)) {
				if (doLog) { logger.info("{}{} ({}): {} allowed on level {} by security relationship for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, accessingUser != null ? accessingUser.getName() : null); }
				return true;
			}

			// new experimental custom permission resultion based on query
			final PropertyKey<String> permissionPropertyKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(Principal.class, "customPermissionQuery" + StringUtils.capitalize(permission.name()));
			final String customPermissionQuery              = accessingUser.getProperty(permissionPropertyKey);

			if (StringUtils.isNotEmpty(customPermissionQuery)) {

				final Map<String, Object> params = new HashMap<>();

				params.put("principalUuid", accessingUser.getUuid());
				params.put("principalId", accessingUser.getUuid());
				params.put("principalType", accessingUser.getType());

				params.put("targetNodeUuid", this.getUuid());
				params.put("targetNodeType", this.getType());

				boolean result = false;
				try {

					final DatabaseService db       = Services.getInstance().getDatabaseService();
					final NativeQuery<Boolean> cpq = db.query(customPermissionQuery, Boolean.class);

					cpq.configure(params);

					result = db.execute(cpq);

				} catch (final Exception ex) {
					logger.error("Error in custom permission resolution", ex);
				}

				logger.info("CPQ (" + permission.name() + ") '" + customPermissionQuery + "': "+ result);

				if (result) {
					return true;
				}

			}

			// Check permissions from domain relationships
			if (resolvePermissions) {

				final Queue<BFSInfo> bfsNodes   = new LinkedList<>();
				final BFSInfo root              = new BFSInfo(null, this);

				// add initial element
				bfsNodes.add(root);

				do {

					final BFSInfo info = bfsNodes.poll();
					if (info != null && info.level < permissionResolutionMaxLevel) {

						final Boolean value = info.node.getPermissionResolutionResult(accessingUser.getUuid(), permission);
						if (value != null) {

							// returning immediately
							if (Boolean.TRUE.equals(value)) {

								// do backtracking
								backtrack(info, accessingUser.getUuid(), permission, true, 0, doLog);

								return true;
							}

						} else {

							if (info.node.hasEffectivePermissions(info, accessingUser, permission, mask, level, alreadyTraversed, bfsNodes, doLog, isCreation)) {

								// do backtracking
								backtrack(info, accessingUser.getUuid(), permission, true, 0, doLog);

								return true;
							}
						}
					}

				} while (!bfsNodes.isEmpty());

				// do backtracking
				backtrack(root, accessingUser.getUuid(), permission, false, 0, doLog);
			}

			// Last: recursively check possible parent principals
			for (Principal parent : accessingUser.getParentsPrivileged()) {

				if (isGranted(permission, parent, mask, level+1, alreadyTraversed, false, doLog, localIncomingSecurityRelationships, isCreation)) {
					return true;
				}
			}
		}

		return false;
	}

	private void backtrack(final BFSInfo info, final String principalId, final Permission permission, final boolean value, final int level, final boolean doLog) {

		final StringBuilder buf = new StringBuilder();

		if (doLog) {

			if (level == 0) {

				if (value) {

					buf.append(permission.name()).append(": granted: ");

				} else {

					buf.append(permission.name()).append(": denied: ");
				}
			}

			buf.append(info.node.getType()).append(" (").append(info.node.getUuid()).append(") --> ");
		}

		info.node.storePermissionResolutionResult(principalId, permission, value);

		// go to parent(s)
		if (info.parent != null) {

			backtrack(info.parent, principalId, permission, value, level+1, doLog);
		}

		if (doLog && level == 0) {
			logger.info(buf.toString());
		}
	}


	private boolean hasEffectivePermissions(final BFSInfo parent, final Principal principal, final Permission permission, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final Queue<BFSInfo> bfsNodes, final boolean doLog, final boolean isCreation) {

		// check nodes here to avoid circles in permission-propagating relationships
		if (alreadyTraversed.contains("Node", getUuid())) {
			return false;
		}

		if (doLog) { logger.info("{}{} ({}): checking {} access on level {} for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, principal != null ? principal.getName() : null); }

		for (final Class<Relation> propagatingType : SchemaRelationshipNode.getPropagatingRelationshipTypes()) {

			// iterate over list of relationships
			final List<Relation> list = Iterables.toList(getRelationshipsAsSuperUser(propagatingType));
			final int count           = list.size();
			final int threshold       = 1000;

			if (count < threshold) {

				for (final Relation source : list) {

					if (source instanceof PermissionPropagation) {

						final PermissionPropagation perm = (PermissionPropagation)source;
						final RelationshipInterface rel  = (RelationshipInterface)source;

						if (doLog) { logger.info("{}{}: checking {} access on level {} via {} for {}", StringUtils.repeat("    ", level), getUuid(), permission.name(), level, rel.getRelType().name(), principal != null ? principal.getName() : null); }

						// check propagation direction vs. evaluation direction
						if (propagationAllowed(this, rel, perm.getPropagationDirection(), doLog)) {

							applyCurrentStep(perm, mask);

							if (mask.allowsPermission(permission)) {

								final AbstractNode otherNode = (AbstractNode)rel.getOtherNode(this);

								if (otherNode.isGranted(permission, principal, mask, level + 1, alreadyTraversed, false, doLog, isCreation)) {

									otherNode.storePermissionResolutionResult(principal.getUuid(), permission, true);

									// break early
									return true;

								} else {

									// add node to BFS queue
									bfsNodes.add(new BFSInfo(parent, otherNode));
								}
							}
						}
					}
				}

			} else if (doLog) {

				logger.warn("Refusing to resolve permissions with {} because there are more than {} nodes.", propagatingType.getSimpleName(), threshold);
			}
		}

		return false;
	}

	/**
	 * Determines whether propagation of permissions is allowed along the given relationship.
	 *
	 * CAUTION: this is a complex situation.
	 *
	 * - we need to determine the EVALUATION DIRECTION, which can be either WITH or AGAINST the RELATIONSHIP DIRECTION
	 * - if we are looking at the START NODE of the relationship, we are evaluating WITH the relationship direction
	 * - if we are looking at the END NODE of the relationship, we are evaluating AGAINST the relationship direction
	 * - the result obtained by the above check must be compared to the PROPAGATION DIRECTION which can be either
	 *   SOURCE_TO_TARGET or TARGET_TO_SOURCE
	 * - a propagation direction of SOURCE_TO_TARGET implies that the permissions of the SOURCE NODE can be applied
	 *   to the TARGET NODE, so if we are evaluating AGAINST the relationship direction, we are good to go
	 * - a propagation direction of TARGET_TO_SOURCE implies that the permissions of that TARGET NODE can be applied
	 *   to the SOURCE NODE, so if we are evaluating WITH the relationship direction, we are good to go
	 *
	 * @param thisNode
	 * @param rel
	 * @param propagationDirection
	 *
	 * @return whether permission resolution can continue along this relationship
	 */
	private boolean propagationAllowed(final AbstractNode thisNode, final RelationshipInterface rel, final PropagationDirection propagationDirection, final boolean doLog) {

		// early exit
		if (propagationDirection.equals(PropagationDirection.Both)) {
			return true;
		}

		// early exit
		if (propagationDirection.equals(PropagationDirection.None)) {
			return false;
		}

		final String sourceNodeId = rel.getSourceNode().getUuid();
		final String thisNodeId   = thisNode.getUuid();

		if (sourceNodeId.equals(thisNodeId)) {

			// evaluation WITH the relationship direction
			switch (propagationDirection) {

				case Out:
					return false;

				case In:
					return true;
			}

		} else {

			// evaluation AGAINST the relationship direction
			switch (propagationDirection) {

				case Out:
					return true;

				case In:
					return false;
			}
		}

		return false;
	}

	private void applyCurrentStep(final PermissionPropagation rel, PermissionResolutionMask mask) {

		switch (rel.getReadPropagation()) {
			case Add:
			case Keep:
				mask.addRead();
				break;

			case Remove:
				mask.removeRead();
				break;

			default: break;
		}

		switch (rel.getWritePropagation()) {
			case Add:
			case Keep:
				mask.addWrite();
				break;

			case Remove:
				mask.removeWrite();
				break;

			default: break;
		}

		switch (rel.getDeletePropagation()) {
			case Add:
			case Keep:
				mask.addDelete();
				break;

			case Remove:
				mask.removeDelete();
				break;

			default: break;
		}

		switch (rel.getAccessControlPropagation()) {
			case Add:
			case Keep:
				mask.addAccessControl();
				break;

			case Remove:
				mask.removeAccessControl();
				break;

			default: break;
		}

		// handle delta properties
		mask.handleProperties(rel.getDeltaProperties());
	}

	private Boolean getPermissionResolutionResult(final String principalId, final Permission permission) {

		Map<String, PermissionResolutionResult> permissionResolutionCache = globalPermissionResolutionCache.get(getUuid());
		if (permissionResolutionCache == null) {

			permissionResolutionCache = new HashMap<>();
			globalPermissionResolutionCache.put(getUuid(), permissionResolutionCache);
		}

		PermissionResolutionResult result = permissionResolutionCache.get(principalId);
		if (result != null) {

			if (permission.equals(Permission.read)) {
				return result.read;
			}

			if (permission.equals(Permission.write)) {
				return result.write;
			}

			if (permission.equals(Permission.delete)) {
				return result.delete;
			}

			if (permission.equals(Permission.accessControl)) {
				return result.accessControl;
			}
		}

		return null;
	}

	private void storePermissionResolutionResult(final String principalId, final Permission permission, final boolean value) {

		Map<String, PermissionResolutionResult> permissionResolutionCache = globalPermissionResolutionCache.get(getUuid());
		if (permissionResolutionCache == null) {

			permissionResolutionCache = new HashMap<>();
			globalPermissionResolutionCache.put(getUuid(), permissionResolutionCache);
		}

		PermissionResolutionResult result = permissionResolutionCache.get(principalId);
		if (result == null) {

			result = new PermissionResolutionResult();
			permissionResolutionCache.put(principalId, result);
		}

		if (permission.equals(Permission.read) && (result.read == null || result.read == false)) {
			result.read = value;
		}

		if (permission.equals(Permission.write) && (result.write == null || result.write == false)) {
			result.write = value;
		}

		if (permission.equals(Permission.delete) && (result.delete == null || result.delete == false)) {
			result.delete = value;
		}

		if (permission.equals(Permission.accessControl) && (result.accessControl == null || result.accessControl == false)) {
			result.accessControl = value;
		}
	}

	private Security getSecurityRelationship(final Principal p, final Map<String, Security> securityRelationships) {

		if (p == null) {

			return null;
		}

		return securityRelationships.get(p.getUuid());
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

		if (p != null) {

			// try filter predicate to speed up things
			final Predicate<GraphObject> principalFilter = (GraphObject value) -> {
				return (p.getUuid().equals(value.getUuid()));
			};

			return getSecurityRelationship(p, mapSecurityRelationshipsMapped(getIncomingRelationshipsAsSuperUser(Security.class, principalFilter)));
		}

		return getSecurityRelationship(p, mapSecurityRelationshipsMapped(getIncomingRelationshipsAsSuperUser(Security.class)));
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		clearCaches();
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		clearCaches();
	}

	@Override
	public void afterCreation(SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterModification(SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterDeletion(SecurityContext securityContext, PropertyMap properties) {
	}

	@Override
	public void ownerModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void securityModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void locationModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = true;

		// the following two checks can be omitted in release 2.4 when Neo4j uniqueness constraints are live
		valid &= ValidationHelper.isValidStringNotBlank(this, id, errorBuffer);

		if (securityContext != null && securityContext.uuidWasSetManually()) {
			valid &= ValidationHelper.isValidGloballyUniqueProperty(this, id, errorBuffer);
		}

		valid &= ValidationHelper.isValidUuid(this, id, errorBuffer);
		valid &= ValidationHelper.isValidStringNotBlank(this, type, errorBuffer);

		return valid;

	}

	@Override
	public boolean isVisibleToPublicUsers() {
		return getVisibleToPublicUsers();
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
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
	public final Date getCreatedDate() {
		return getProperty(createdDate);
	}

	@Override
	public final Date getLastModifiedDate() {
		return getProperty(lastModifiedDate);
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
		return setProperty(key, value, false);
	}

	@Override
	public <T> Object setProperty(final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {

		// clear function property cache in security context since we are about to invalidate past results
		if (securityContext != null) {
			securityContext.getContextStore().clearFunctionPropertyCache();
		}

		// allow setting of ID without permissions
		if (!key.equals(GraphObject.id)) {

			if (!isGranted(Permission.write, securityContext, isCreation)) {

				internalSystemPropertiesUnlocked = false;
				readOnlyPropertiesUnlocked       = false;

				final Principal currentUser = securityContext.getUser(false);
				String user = null;

				if (currentUser == null) {
					user = securityContext.isSuperUser() ? "superuser" : "anonymous";
				} else {
					user = currentUser.getProperty(AbstractNode.id);
				}

				throw new FrameworkException(403, "Modification of node " + this.getProperty(AbstractNode.id) + " with type " + this.getProperty(AbstractNode.type) + " by user " + user + " not permitted.");
			}
		}

		try {

			// no need to check previous value when creating a node
			T oldValue = isCreation ? null : getProperty(key);

			// no old value exists  OR  old value exists and is NOT equal => set property
			if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!oldValue.equals(value)) || (key instanceof FunctionProperty)) ) {

				return setPropertyInternal(key, value);
			}

		} finally {

			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;
		}

		return null;
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		setProperties(securityContext, properties, false);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

		if (!isGranted(Permission.write, securityContext, isCreation)) {

			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;

			final Principal currentUser = securityContext.getUser(false);
			String user = null;

			if (currentUser == null) {
				user = securityContext.isSuperUser() ? "superuser" : "anonymous";
			} else {
				user = currentUser.getProperty(AbstractNode.id);
			}

			throw new FrameworkException(403, "Modification of node " + this.getProperty(AbstractNode.id) + " with type " + this.getProperty(AbstractNode.type) + " by user " + user + " not permitted.");
		}

		for (final PropertyKey key : properties.keySet()) {

			final Object oldValue = isCreation ? null : getProperty(key);
			final Object value    = properties.get(key);

			// no old value exists  OR  old value exists and is NOT equal => set property
			if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!oldValue.equals(value)) || (key instanceof FunctionProperty)) ) {

				if (!key.equals(GraphObject.id)) {

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
		}

		NodeInterface.super.setPropertiesInternal(securityContext, properties, isCreation);
	}

	private <T> Object setPropertyInternal(final PropertyKey<T> key, final T value) throws FrameworkException {

		if (key == null) {

			logger.error("Tried to set property with null key (action was denied)");

			throw new FrameworkException(422, "Tried to set property with null key (action was denied)", new NullArgumentToken(getClass().getSimpleName(), base));

		}

		try {

			// check for system properties
			if (key.isSystemInternal() && !internalSystemPropertiesUnlocked) {

				throw new FrameworkException(422, "Property " + key.jsonName() + " is an internal system property", new InternalSystemPropertyToken(getClass().getSimpleName(), key));
			}

			// check for read-only properties
			if ((key.isReadOnly() || key.isWriteOnce()) && !readOnlyPropertiesUnlocked && !securityContext.isSuperUser()) {

				throw new FrameworkException(422, "Property " + key.jsonName() + " is read-only", new ReadOnlyPropertyToken(getClass().getSimpleName(), key));
			}

			return key.setProperty(securityContext, this, value);

		} finally {

			// unconditionally lock read-only properties after every write (attempt) to avoid security problems
			// since we made "unlock_readonly_properties_once" available through scripting
			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;
		}

	}

	public static void clearRelationshipTemplateInstanceCache() {
		relationshipTemplateInstanceCache.clear();
	}

	public static void clearCaches() {
		globalPermissionResolutionCache.clear();
		isGrantedResultCache.clear();
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
	public String getPropertyWithVariableReplacement(ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {

		final Object value = getProperty(key);
		String result      = null;

		try {

			result = Scripting.replaceVariables(renderContext, this, value, true, key.jsonName());

		} catch (Throwable t) {

			logger.warn("Scripting error in {} {}:\n{}", key.dbName(), getUuid(), value, t);

		}

		return result;
	}

	public final Object getPath(final SecurityContext currentSecurityContext) {

		if (rawPathSegmentId != null) {

			final Relationship rel = StructrApp.getInstance(currentSecurityContext).getDatabaseService().getRelationshipById(rawPathSegmentId);
			if (rel != null) {

				final RelationshipFactory factory = new RelationshipFactory(currentSecurityContext);
				return factory.instantiate(rel);
			}
		}

		return null;
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

		hints.reportUsedKey(key, row, column);

		switch (key) {

			case "owner":
				hints.reportExistingKey(key);
				return getOwnerNode();

			case "_path":
				hints.reportExistingKey(key);
				return getPath(actionContext.getSecurityContext());

			default:

				// evaluate object value or return default
				final PropertyKey propertyKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, key, false);
				if (propertyKey != null) {

					hints.reportExistingKey(key);

					final Object value = getProperty(propertyKey, actionContext.getPredicate());
					if (value != null) {

						return value;
					}
				}

				final ContextStore contextStore      = actionContext.getContextStore();
				final Map<String, Object> parameters = contextStore.getTemporaryParameters();

				final Object value = invokeMethod(actionContext.getSecurityContext(), key, parameters, false, hints);
				if (value != null) {

					return value;
				}

				return Function.numberOrString(defaultValue);
		}
	}

	@Override
	public final Object invokeMethod(final SecurityContext securityContext, final String methodName, final Map<String, Object> propertySet, final boolean throwExceptionForUnknownMethods, final EvaluationHints hints) throws FrameworkException {

		final Method method = StructrApp.getConfiguration().getExportedMethodsForType(entityType).get(methodName);
		if (method != null) {

			hints.reportExistingKey(methodName);

			return AbstractNode.invokeMethod(securityContext, method, this, propertySet, hints);
		}

		// in the case of REST access we want to know if the method exists or not
		if (throwExceptionForUnknownMethods) {
			throw new FrameworkException(400, "Method " + methodName + " not found in type " + getType());
		}

		return null;
	}

	private Map<String, Security> mapSecurityRelationshipsMapped(final Iterable<Security> src) {

		final Map<String, Security> map = new HashMap<>();

		for (final Security sec : src) {

			map.put(sec.getSourceNodeId(), sec);
		}

		return map;
	}

	@Override
	public final void grant(Permission permission, Principal principal) throws FrameworkException {
		grant(Collections.singleton(permission), principal, securityContext);
	}

	@Override
	public final void grant(final Set<Permission> permissions, Principal principal) throws FrameworkException {
		grant(permissions, principal, securityContext);
	}

	@Override
	public final void grant(final Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, "Access control not permitted");
		}

		clearCaches();

		Security secRel = getSecurityRelationship(principal);
		if (secRel == null) {

			try {

				Set<String> permissionSet = new HashSet<>();

				for (Permission permission : permissions) {

					permissionSet.add(permission.name());
				}

				// ensureCardinality is not neccessary here
				final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
				final PropertyMap properties           = new PropertyMap();
				superUserContext.disablePreventDuplicateRelationships();

				// performance improvement for grant(): add properties to the CREATE call that would
				// otherwise be set in separate calls later in the transaction.
				properties.put(Security.principalId,                    principal.getUuid());
				properties.put(Security.accessControllableId,           getUuid());
				properties.put(Security.allowed,                        permissionSet.toArray(new String[permissionSet.size()]));

				StructrApp.getInstance(superUserContext).create(principal, (NodeInterface)this, Security.class, properties);

			} catch (FrameworkException ex) {

				logger.error("Could not create security relationship!", ex);
			}

		} else {

			secRel.addPermissions(permissions);
		}
	}

	@Override
	public final void revoke(Permission permission, Principal principal) throws FrameworkException {
		revoke(Collections.singleton(permission), principal, securityContext);
	}

	@Override
	public final void revoke(final Set<Permission> permissions, Principal principal) throws FrameworkException {
		revoke(permissions, principal, securityContext);
	}

	@Override
	public final void revoke(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, "Access control not permitted");
		}

		clearCaches();

		Security secRel = getSecurityRelationship(principal);
		if (secRel != null) {

			secRel.removePermissions(permissions);
		}
	}


	@Override
	public final void setAllowed(Set<Permission> permissions, Principal principal) throws FrameworkException {
		setAllowed(permissions, principal, securityContext);
	}

	@Override
	public final void setAllowed(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, "Access control not permitted");
		}

		clearCaches();

		final Set<String> permissionSet = new HashSet<>();

		for (Permission permission : permissions) {

			permissionSet.add(permission.name());
		}

		Security secRel = getSecurityRelationship(principal);
		if (secRel == null) {

			if (permissions.size() > 0) {

				try {

					// ensureCardinality is not neccessary here
					final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
					final PropertyMap properties           = new PropertyMap();
					superUserContext.disablePreventDuplicateRelationships();

					// performance improvement for grant(): add properties to the CREATE call that would
					// otherwise be set in separate calls later in the transaction.
					properties.put(Security.principalId,                    principal.getUuid());
					properties.put(Security.accessControllableId,           getUuid());
					properties.put(Security.allowed,                        permissionSet.toArray(new String[permissionSet.size()]));

					StructrApp.getInstance(superUserContext).create(principal, (NodeInterface)this, Security.class, properties);

				} catch (FrameworkException ex) {

					logger.error("Could not create security relationship!", ex);
				}
			}

		} else {
			secRel.setAllowed(permissionSet);
		}
	}

	@Override
	public final void setRawPathSegmentId(final Identity rawPathSegmentId) {
		this.rawPathSegmentId = rawPathSegmentId;
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
	public List<Security> getSecurityRelationships() {

		final List<Security> grants = Iterables.toList(getIncomingRelationshipsAsSuperUser(Security.class));

		// sort list by principal name (important for diff'able export)
		Collections.sort(grants, (o1, o2) -> {

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
		});

		return grants;
	}

	@Override
	public boolean changelogEnabled() {
		return true;
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

	public String getCreatedBy() {
		return getProperty(AbstractNode.createdBy);
	}


	public String getLastModifiedBy() {
		return getProperty(AbstractNode.lastModifiedBy);
	}

	@Override
	public synchronized Map<String, Object> getTemporaryStorage() {

		if (tmpStorageContainer == null) {
			tmpStorageContainer = new LinkedHashMap<>();
		}

		return tmpStorageContainer;
	}

	protected boolean isGenericNode() {
		return false;
	}

	protected boolean allowedBySchema(final Principal principal, final Permission permission) {
		return false;
	}

	// ----- static methods -----
	public static Object invokeMethod(final SecurityContext securityContext, final Method method, final Object entity, final Map<String, Object> propertySet, final EvaluationHints hints) throws FrameworkException {

		try {

			// new structure: first parameter is always securityContext and second parameter can be Map (for dynamically defined methods)
			if (method.getParameterTypes().length == 2 && method.getParameterTypes()[0].isAssignableFrom(SecurityContext.class) && method.getParameterTypes()[1].equals(Map.class)) {
				final Object[] args = new Object[] { securityContext };
				return method.invoke(entity, ArrayUtils.add(args, propertySet));
			}

			// second try: extracted parameter list
			final Object[] args = extractParameters(propertySet, method.getParameterTypes());

			return method.invoke(entity, ArrayUtils.add(args, 0, securityContext));

		} catch (InvocationTargetException itex) {

			final Throwable cause = itex.getCause();

			if (cause instanceof AssertException) {

				final AssertException e = (AssertException)cause;
				throw new FrameworkException(e.getStatus(), e.getMessage());
			}

			if (cause instanceof FrameworkException) {

				throw (FrameworkException) cause;
			}

		} catch (IllegalAccessException | IllegalArgumentException  t) {

			logger.warn("Unable to invoke method {}: {}", method.getName(), t.getMessage());
		}

		return null;
	}

	private static Object[] extractParameters(Map<String, Object> properties, Class[] parameterTypes) {

		final List<Object> values = new ArrayList<>(properties.values());
		final List<Object> parameters = new ArrayList<>();
		int index = 0;

		// only try to convert when both lists have equal size
		// subtract one because securityContext is default and not provided by user
		if (values.size() == (parameterTypes.length - 1)) {

			for (final Class parameterType : parameterTypes) {

				// skip securityContext
				if (!parameterType.isAssignableFrom(SecurityContext.class)) {

					final Object value = convert(values.get(index++), parameterType);
					if (value != null) {

						parameters.add(value);
					}
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
	private static Object convert(Object value, Class type) {

		// short-circuit
		if (type.isAssignableFrom(value.getClass())) {
			return value;
		}

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

		} else if (value instanceof Boolean) {

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

	// ----- nested classes -----
	private static class AlreadyTraversed {

		private Map<String, Set<String>> sets = new LinkedHashMap<>();

		public boolean contains(final String key, final String uuid) {

			Set<String> set = sets.get(key);
			if (set == null) {

				set = new HashSet<>();
				sets.put(key, set);
			}

			return !set.add(uuid);
		}

		public int size(final String key) {

			final Set<String> set = sets.get(key);
			if (set != null) {

				return set.size();
			}

			return 0;
		}
	}

	private static class BFSInfo {

		public AbstractNode node      = null;
		public BFSInfo parent         = null;
		public int level              = 0;

		public BFSInfo(final BFSInfo parent, final AbstractNode node) {

			this.parent = parent;
			this.node   = node;

			if (parent != null) {
				this.level  = parent.level+1;
			}
		}
	}

	private static class PermissionResolutionResult {

		Boolean read          = null;
		Boolean write         = null;
		Boolean delete        = null;
		Boolean accessControl = null;
	}
}
