/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.graph.*;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.common.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.util.*;

/**
 * Abstract base class for all node entities in Structr.
 */
public abstract class AbstractNode extends AbstractGraphObject<Node> implements NodeInterface, AccessControllable {

	private static final int permissionResolutionMaxLevel                                                     = Settings.ResolutionDepth.getValue();
	private static final Logger logger                                                                        = LoggerFactory.getLogger(AbstractNode.class.getName());
	private static final FixedSizeCache<String, Boolean> isGrantedResultCache                                 = new FixedSizeCache<>("Grant result cache", 100000);
	private static final FixedSizeCache<String, Object> relationshipTemplateInstanceCache                     = new FixedSizeCache<>("Relationship template cache", 1000);
	private static final Map<String, Map<String, PermissionResolutionResult>> globalPermissionResolutionCache = new HashMap<>();

	/*
	public static final View defaultView = new View(AbstractNode.class, PropertyView.Public, id, type, name);

	public static final View uiView = new View(AbstractNode.class, PropertyView.Ui,
		id, name, owner, type, createdBy, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers
	);

	 */

	private Identity rawPathSegmentId             = null;
	protected PrincipalInterface cachedOwnerNode  = null;

	public AbstractNode() {
	}

	public AbstractNode(SecurityContext securityContext, final Node dbNode, final Class entityType, final long sourceTransactionId) {
		init(securityContext, dbNode, entityType, sourceTransactionId);
	}

	@Override
	public void onNodeCreation(final SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void onNodeInstantiation(final boolean isCreation) {
		this.cachedUuid = getProperty(traits.key("id"));
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public long getSourceTransactionId() {
		return sourceTransactionId;
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

		if (getNode() == null) {

			return (super.hashCode());
		}

		return getNode().getId().hashCode();

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
	 * Get name from underlying db node
	 *
	 * If name is null, return node id as fallback
	 */
	@Override
	public final String getName() {

		String _name = getProperty(AbstractNode.name);
		if (_name == null) {

			_name = getUuid();
		}

		return _name;
	}

	@Override
	public final Node getNode() {
		return getPropertyContainer();
	}

	@Override
	public boolean isDeleted() {
		return TransactionCommand.getCurrentTransaction().isNodeDeleted(id.getId());
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {

		if (getNode() != null && type != null && targetNode != null) {
			return getNode().hasRelationshipTo(type, targetNode.getNode());
		}

		return false;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> R getRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {

		if (getNode() != null && type != null && targetNode != null) {

			final RelationshipFactory<A, B, R> factory = new RelationshipFactory<>(securityContext);
			final Relationship rel                     = getNode().getRelationshipTo(type, targetNode.getNode());

			if (rel != null) {

				return factory.adapt(rel);
			}
		}

		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationships() {
		return new IterableAdapter<>(getNode().getRelationships(), new RelationshipFactory<>(securityContext));
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<RelationshipInterface<A, B>> getRelationships(final Class<R> type) {

		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                                                     = getRelationshipForType(type);
		final Direction direction                                            = template.getDirectionForType(entityType);
		final RelationshipType relType                                       = template;

		return new IterableAdapter<>(getNode().getRelationships(direction, relType), factory);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationship(final Class<R> type) {

		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                                                     = getRelationshipForType(type);
		final Relationship relationship                                      = template.getSource().getRawSource(securityContext, getNode(), null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationshipAsSuperUser(final Class<R> type) {

		final SecurityContext suContext                                      = SecurityContext.getSuperUserInstance();
		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                                                     = getRelationshipForType(type);
		final Relationship relationship                                      = template.getSource().getRawSource(suContext, getNode(), null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationships(final Class<R> type) {

		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                                                     = getRelationshipForType(type);

		return new IterableAdapter<>(template.getSource().getRawSource(securityContext, getNode(), null), factory);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationship(final Class<R> type) {

		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                                                     = getRelationshipForType(type);
		final Relationship relationship                                      = template.getTarget().getRawSource(securityContext, getNode(), null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<RelationshipInterface<A, B>> getOutgoingRelationships(final Class<R> type) {

		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);

		return new IterableAdapter<>(template.getTarget().getRawSource(securityContext, getNode(), null), factory);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getIncomingRelationships() {
		return new IterableAdapter<>(getNode().getRelationships(Direction.INCOMING), new RelationshipFactory<>(securityContext));
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getOutgoingRelationships() {
		return new IterableAdapter<>(getNode().getRelationships(Direction.OUTGOING), new RelationshipFactory<>(securityContext));
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationshipsAsSuperUser() {
		return new IterableAdapter<>(getNode().getRelationships(), new RelationshipFactory<>(SecurityContext.getSuperUserInstance()));
	}

	protected final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationshipsAsSuperUser(final Class<R> type) {
		return getIncomingRelationshipsAsSuperUser(type, null);
	}

	protected final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationshipsAsSuperUser(final Class<R> type, final Predicate<GraphObject> predicate) {

		final SecurityContext suContext                                      = SecurityContext.getSuperUserInstance();
		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                                                     = getRelationshipForType(type);

		return new IterableAdapter<>(template.getSource().getRawSource(suContext, getNode(), predicate), factory);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationshipAsSuperUser(final Class<R> type) {

		final SecurityContext suContext                                      = SecurityContext.getSuperUserInstance();
		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                                                     = getRelationshipForType(type);
		final Relationship relationship                                      = template.getTarget().getRawSource(suContext, getNode(), null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	protected final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<RelationshipInterface<A, B>> getRelationshipsAsSuperUser(final Class<R> type) {

		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
		final R template                                                     = getRelationshipForType(type);
		final Direction direction                                            = template.getDirectionForType(entityType);
		final RelationshipType relType                                       = template;

		return new IterableAdapter<>(getNode().getRelationships(direction, relType), factory);
	}

	/**
	 * Returns the owner node of this node, following an INCOMING OWNS
	 * relationship.
	 *
	 * @return the owner node of this node
	 */
	@Override
	public final PrincipalInterface getOwnerNode() {

		if (cachedOwnerNode == null) {

			final RelationshipInterface<PrincipalInterface, NodeInterface> ownership = getIncomingRelationshipAsSuperUser(PrincipalOwnsNode.class);
			if (ownership != null) {

				cachedOwnerNode = ownership.getSourceNode();
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
		return AbstractNode.getRelationshipForType(type).getSource().hasElements(securityContext, getNode(), null);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Class<R> type) {
		return AbstractNode.getRelationshipForType(type).getTarget().hasElements(securityContext, getNode(), null);
	}

	// ----- interface AccessControllable -----
	@Override
	public boolean isGranted(final Permission permission, final SecurityContext context) {
		return isGranted(permission, context, false);
	}

	@Override
	public boolean isGranted(final Permission permission, final SecurityContext context, final boolean isCreation) {

		// super user can do everything
		if (context != null && context.isSuperUser()) {
			return true;
		}

		PrincipalInterface accessingUser = null;
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

	private boolean isGranted(final Permission permission, final PrincipalInterface accessingUser, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final boolean resolvePermissions, final boolean doLog, final boolean isCreation) {
		return isGranted(permission, accessingUser, mask, level, alreadyTraversed, resolvePermissions, doLog, null, isCreation);
	}

	private boolean isGranted(final Permission permission, final PrincipalInterface accessingUser, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final boolean resolvePermissions, final boolean doLog, final Map<String, RelationshipInterface<PrincipalInterface, NodeInterface>> incomingSecurityRelationships, final boolean isCreation) {

		if (level > 300) {
			logger.warn("Aborting recursive permission resolution for {} on {} because of recursion level > 300, this is quite likely an infinite loop.", permission.name(), getType() + "(" + getUuid() + ")");
			return false;
		}

		if (doLog) { logger.info("{}{} ({}): {} check on level {} for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, accessingUser != null ? accessingUser.getName() : null); }

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

		final PrincipalInterface _owner = getOwnerNode();
		final boolean hasOwner          = (_owner != null);

		if (isCreation && (accessingUser == null || accessingUser.equals(this) || accessingUser.equals(_owner) ) ) {
			return true;
		}

		// allow accessingUser to access itself, but not parents etc.
		if (this.equals(accessingUser) && (level == 0 || (permission.equals(Permission.read) && level > 0))) {
			return true;
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

			final Map<String, RelationshipInterface<PrincipalInterface, NodeInterface>> localIncomingSecurityRelationships = incomingSecurityRelationships != null ? incomingSecurityRelationships : mapSecurityRelationshipsMapped(getIncomingRelationshipsAsSuperUser(Security.class));
			final RelationshipInterface<PrincipalInterface, NodeInterface> security = getSecurityRelationship(accessingUser, localIncomingSecurityRelationships);

			if (security != null && security.isAllowed(permission)) {
				if (doLog) { logger.info("{}{} ({}): {} allowed on level {} by security relationship for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, accessingUser != null ? accessingUser.getName() : null); }
				return true;
			}

			for (PrincipalInterface parent : accessingUser.getParentsPrivileged()) {

				if (isGranted(permission, parent, mask, level+1, alreadyTraversed, false, doLog, localIncomingSecurityRelationships, isCreation)) {
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


	private boolean hasEffectivePermissions(final BFSInfo parent, final PrincipalInterface principal, final Permission permission, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final Queue<BFSInfo> bfsNodes, final boolean doLog, final boolean isCreation) {

		// check nodes here to avoid circles in permission-propagating relationships
		if (alreadyTraversed.contains("Node", getUuid())) {
			return false;
		}

		if (doLog) { logger.info("{}{} ({}): checking {} access on level {} for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, principal != null ? principal.getName() : null); }

		final Node node                = getNode();
		final Map<String, Long> degree = node.getDegree();

		for (final String type : degree.keySet()) {

			final Class propagatingType = StructrApp.getConfiguration().getRelationshipEntityClass(type);

			if (propagatingType != null && PermissionPropagation.class.isAssignableFrom(propagatingType)) {

				// iterate over list of relationships
				final List<RelationshipInterface> list = Iterables.toList(getRelationshipsAsSuperUser(propagatingType));
				final int count = list.size();
				final int threshold = 1000;

				if (count < threshold) {

					for (final RelationshipInterface source : list) {

						if (source instanceof PermissionPropagation perm) {

							if (doLog) {
								logger.info("{}{}: checking {} access on level {} via {} for {}", StringUtils.repeat("    ", level), getUuid(), permission.name(), level, source.getRelType().name(), principal != null ? principal.getName() : null);
							}

							// check propagation direction vs. evaluation direction
							if (propagationAllowed(this, source, perm.getPropagationDirection(), doLog)) {

								applyCurrentStep(perm, mask);

								if (mask.allowsPermission(permission)) {

									final AbstractNode otherNode = (AbstractNode) source.getOtherNode(this);

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

	private RelationshipInterface<PrincipalInterface, NodeInterface> getSecurityRelationship(final PrincipalInterface p, final Map<String, RelationshipInterface<PrincipalInterface, NodeInterface>> securityRelationships) {

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
	public final RelationshipInterface<PrincipalInterface, NodeInterface> getSecurityRelationship(final PrincipalInterface p) {

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
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		clearCaches();
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		clearCaches();
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

				instance = type.getDeclaredConstructor().newInstance();
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
				final PropertyKey propertyKey = traits.key(key);
				if (propertyKey != null) {

					hints.reportExistingKey(key);

					final Object value = getProperty(propertyKey, actionContext.getPredicate());
					if (value != null) {

						return value;
					}
				}

				final AbstractMethod method = Methods.resolveMethod(traits, key);
				if (method != null) {

					final ContextStore contextStore = actionContext.getContextStore();
					final Map<String, Object> temp  = contextStore.getTemporaryParameters();
					final Arguments arguments       = Arguments.fromMap(temp);

					return method.execute(actionContext.getSecurityContext(), this, arguments, hints);
				}

				return Function.numberOrString(defaultValue);
		}
	}

	private Map<String, RelationshipInterface<PrincipalInterface, NodeInterface>> mapSecurityRelationshipsMapped(final Iterable<RelationshipInterface<PrincipalInterface, NodeInterface>> src) {

		final Map<String, RelationshipInterface<PrincipalInterface, NodeInterface>> map = new HashMap<>();

		for (final RelationshipInterface<PrincipalInterface, NodeInterface> sec : src) {

			map.put(sec.getSourceNodeId(), sec);
		}

		return map;
	}

	@Override
	public final void grant(Permission permission, PrincipalInterface principal) throws FrameworkException {
		grant(Collections.singleton(permission), principal, securityContext);
	}

	@Override
	public final void grant(final Set<Permission> permissions, PrincipalInterface principal) throws FrameworkException {
		grant(permissions, principal, securityContext);
	}

	@Override
	public final void grant(final Set<Permission> permissions, PrincipalInterface principal, SecurityContext ctx) throws FrameworkException {

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, getAccessControlNotPermittedExceptionString("grant", permissions, principal, ctx));
		}

		clearCaches();

		RelationshipInterface<PrincipalInterface, NodeInterface> secRel = getSecurityRelationship(principal);
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
	public final void revoke(Permission permission, PrincipalInterface principal) throws FrameworkException {
		revoke(Collections.singleton(permission), principal, securityContext);
	}

	@Override
	public final void revoke(final Set<Permission> permissions, PrincipalInterface principal) throws FrameworkException {
		revoke(permissions, principal, securityContext);
	}

	@Override
	public final void revoke(Set<Permission> permissions, PrincipalInterface principal, SecurityContext ctx) throws FrameworkException {

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, getAccessControlNotPermittedExceptionString("revoke", permissions, principal, ctx));
		}

		clearCaches();

		RelationshipInterface<PrincipalInterface, NodeInterface> secRel = getSecurityRelationship(principal);
		if (secRel != null) {

			secRel.removePermissions(permissions);
		}
	}


	@Override
	public final void setAllowed(Set<Permission> permissions, PrincipalInterface principal) throws FrameworkException {
		setAllowed(permissions, principal, securityContext);
	}

	@Override
	public final void setAllowed(Set<Permission> permissions, PrincipalInterface principal, SecurityContext ctx) throws FrameworkException {

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, getAccessControlNotPermittedExceptionString("set", permissions, principal, ctx));
		}

		clearCaches();

		final Set<String> permissionSet = new HashSet<>();

		for (Permission permission : permissions) {

			permissionSet.add(permission.name());
		}

		RelationshipInterface<PrincipalInterface, NodeInterface> secRel = getSecurityRelationship(principal);
		if (secRel == null) {

			if (!permissions.isEmpty()) {

				try {

					// ensureCardinality is not necessary here
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

	@Override
	public List<RelationshipInterface<PrincipalInterface, NodeInterface>> getSecurityRelationships() {

		final List<RelationshipInterface<PrincipalInterface, NodeInterface>> grants = Iterables.toList(getIncomingRelationshipsAsSuperUser(Security.class));

		// sort list by principal name (important for diff'able export)
		Collections.sort(grants, (o1, o2) -> {

			final PrincipalInterface p1 = o1.getSourceNode();
			final PrincipalInterface p2 = o2.getSourceNode();
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
	public final NodeInterface getSyncNode() {
		return this;
	}

	@Override
	public final RelationshipInterface getSyncRelationship() {
		throw new ClassCastException(this.getClass() + " cannot be cast to org.structr.core.graph.RelationshipInterface");
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

	protected boolean allowedBySchema(final PrincipalInterface principal, final Permission permission) {
		return false;
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
