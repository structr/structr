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
package org.structr.core.traits;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Security;
import org.structr.core.entity.relationship.Ownership;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.property.PropertyMap;

import java.util.*;

public class AccessControllableTrait extends NodeTraitImpl<AccessControllable> implements AccessControllable {

	private static final Logger logger = LoggerFactory.getLogger(AccessControllableTrait.class);

	private static final FixedSizeCache<String, Boolean> isGrantedResultCache                                 = new FixedSizeCache<>("Grant result cache", 100000);
	private static final Map<String, Map<String, PermissionResolutionResult>> globalPermissionResolutionCache = new HashMap<>();
	private static final int permissionResolutionMaxLevel                                                     = Settings.ResolutionDepth.getValue();

	protected Principal cachedOwnerNode = null;

	static {

		final Trait<AccessControllable> trait = Trait.create(AccessControllable.class, n -> new AccessControllableTrait(n));

	}

	public AccessControllableTrait(final PropertyContainer node) {
		super(node);
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		return getProperty(key("visibleToPublicUsers"));
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		return getProperty(key("visibleToAuthenticatedUsers"));
	}

	@Override
	public final boolean isHidden() {
		return getProperty(key("hidden"));
	}

	@Override
	public final Date getCreatedDate() {
		return getProperty(key("createdDate"));
	}

	@Override
	public final Date getLastModifiedDate() {
		return getProperty(key("lastModifiedDate"));
	}

	@Override
	public boolean isGranted(final Permission permission, final SecurityContext context) {
		return isGranted(permission, context, false);
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

			final Ownership ownership = getIncomingRelationshipAsSuperUser(Trait.of(PrincipalOwnsNode.class));
			if (ownership != null) {

				Principal principal = ownership.getSourceNode();
				cachedOwnerNode     = principal;
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

	@Override
	public boolean isGranted(final Permission permission, final SecurityContext context, final boolean isCreation) {

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

		final Principal _owner = getOwnerNode();
		final boolean hasOwner = (_owner != null);

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

			final Map<String, Security> localIncomingSecurityRelationships = incomingSecurityRelationships != null ? incomingSecurityRelationships : mapSecurityRelationshipsMapped(getIncomingRelationshipsAsSuperUser(Trait.of(Security.class)));
			final Security security                                        = getSecurityRelationship(accessingUser, localIncomingSecurityRelationships);

			if (security != null && security.isAllowed(permission)) {
				if (doLog) { logger.info("{}{} ({}): {} allowed on level {} by security relationship for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, accessingUser.getName()); }
				return true;
			}

			for (Principal parent : accessingUser.getParentsPrivileged()) {

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

	private void backtrack(final AccessControllableTrait.BFSInfo info, final String principalId, final Permission permission, final boolean value, final int level, final boolean doLog) {

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


	private boolean hasEffectivePermissions(final BFSInfo parent, final Principal principal, final Permission permission, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final Queue<AccessControllableTrait.BFSInfo> bfsNodes, final boolean doLog, final boolean isCreation) {

		// check nodes here to avoid circles in permission-propagating relationships
		if (alreadyTraversed.contains("Node", getUuid())) {
			return false;
		}

		if (doLog) { logger.info("{}{} ({}): checking {} access on level {} for {}", StringUtils.repeat("    ", level), getUuid(), getType(), permission.name(), level, principal != null ? principal.getName() : null); }

		final Node node                = getNode();
		final Map<String, Long> degree = node.getDegree();

		for (final String type : degree.keySet()) {

			final Trait<? extends Relation> propagatingType = Trait.of(type); //StructrApp.getConfiguration().getRelationshipEntityClass(type);

			// FIXME
			if (propagatingType != null && PermissionPropagation.class.isAssignableFrom(propagatingType.getClass())) {

				// iterate over list of relationships
				final List<Relation> list = Iterables.toList(getRelationshipsAsSuperUser(propagatingType));
				final int count = list.size();
				final int threshold = 1000;

				if (count < threshold) {

					for (final Relation source : list) {

						if (source instanceof PermissionPropagation perm) {

							if (doLog) {
								logger.info("{}{}: checking {} access on level {} via {} for {}", StringUtils.repeat("    ", level), getUuid(), permission.name(), level, source.getRelType().name(), principal != null ? principal.getName() : null);
							}

							// check propagation direction vs. evaluation direction
							if (propagationAllowed(this, source, perm.getPropagationDirection(), doLog)) {

								applyCurrentStep(perm, mask);

								if (mask.allowsPermission(permission)) {

									// this is ugly.. :(
									final AccessControllableTrait otherNode = (AccessControllableTrait) source.getOtherNode(this);

									if (otherNode.isGranted(permission, principal, mask, level + 1, alreadyTraversed, false, doLog, isCreation)) {

										otherNode.storePermissionResolutionResult(principal.getUuid(), permission, true);

										// break early
										return true;

									} else {

										// add node to BFS queue
										bfsNodes.add(new AccessControllableTrait.BFSInfo(parent, otherNode));
									}
								}
							}
						}
					}

				} else if (doLog) {

					logger.warn("Refusing to resolve permissions with {} because there are more than {} nodes.", propagatingType.getName(), threshold);
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
	private boolean propagationAllowed(final NodeTrait thisNode, final RelationshipTrait rel, final PropagationDirection propagationDirection, final boolean doLog) {

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
			final Predicate<GraphTrait> principalFilter = value -> p.getUuid().equals(value.getUuid());

			return getSecurityRelationship(p, mapSecurityRelationshipsMapped(getIncomingRelationshipsAsSuperUser(Trait.of(Security.class), principalFilter)));
		}

		return getSecurityRelationship(p, mapSecurityRelationshipsMapped(getIncomingRelationshipsAsSuperUser(Trait.of(Security.class))));
	}

	@Override
	public final void grant(final Permission permission, final Principal principal) throws FrameworkException {
		grant(Collections.singleton(permission), principal, securityContext);
	}

	@Override
	public final void grant(final Set<Permission> permissions, final Principal principal) throws FrameworkException {
		grant(permissions, principal, securityContext);
	}

	@Override
	public final void grant(final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException {

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, getAccessControlNotPermittedExceptionString("grant", permissions, principal, ctx));
		}

		//clearCaches();

		Security secRel = getSecurityRelationship(principal);
		if (secRel == null) {

			try {

				Set<String> permissionSet = new HashSet<>();

				for (Permission permission : permissions) {

					permissionSet.add(permission.name());
				}

				// ensureCardinality is not neccessary here
				final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
				final Trait<Security> trait            = Trait.of(Security.class);
				final PropertyMap properties           = new PropertyMap();

				superUserContext.disablePreventDuplicateRelationships();

				// performance improvement for grant(): add properties to the CREATE call that would
				// otherwise be set in separate calls later in the transaction.
				properties.put(trait.key("principalId"),          principal.getUuid());
				properties.put(trait.key("accessControllableId"), getUuid());
				properties.put(trait.key("allowed"),              permissionSet.toArray(new String[permissionSet.size()]));

				StructrApp.getInstance(superUserContext).create(principal, this, Traits.of("Security"), properties);

			} catch (FrameworkException ex) {

				logger.error("Could not create security relationship!", ex);
			}

		} else {

			secRel.addPermissions(permissions);
		}
	}

	@Override
	public final void revoke(final Permission permission, final Principal principal) throws FrameworkException {
		revoke(Collections.singleton(permission), principal, securityContext);
	}

	@Override
	public final void revoke(final Set<Permission> permissions, Principal principal) throws FrameworkException {
		revoke(permissions, principal, securityContext);
	}

	@Override
	public final void revoke(final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException {

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, getAccessControlNotPermittedExceptionString("revoke", permissions, principal, ctx));
		}

		//clearCaches();

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
			throw new FrameworkException(403, getAccessControlNotPermittedExceptionString("set", permissions, principal, ctx));
		}

		//clearCaches();

		final Set<String> permissionSet = new HashSet<>();

		for (Permission permission : permissions) {

			permissionSet.add(permission.name());
		}

		Security secRel = getSecurityRelationship(principal);
		if (secRel == null) {

			if (!permissions.isEmpty()) {

				try {

					// ensureCardinality is not necessary here
					final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
					final Trait<Security> trait            = Trait.of(Security.class);
					final PropertyMap properties           = new PropertyMap();

					superUserContext.disablePreventDuplicateRelationships();

					// performance improvement for grant(): add properties to the CREATE call that would
					// otherwise be set in separate calls later in the transaction.
					properties.put(trait.key("principalId"),           principal.getUuid());
					properties.put(trait.key("accessControllableId"),  getUuid());
					properties.put(trait.key("allowed"),               permissionSet.toArray(new String[permissionSet.size()]));

					// FIXME
					StructrApp.getInstance(superUserContext).create(principal, this, Traits.of("Security"), properties);

				} catch (FrameworkException ex) {

					logger.error("Could not create security relationship!", ex);
				}
			}

		} else {
			secRel.setAllowed(permissionSet);
		}
	}

	private Map<String, Security> mapSecurityRelationshipsMapped(final Iterable<Security> src) {

		final Map<String, Security> map = new HashMap<>();

		for (final Security sec : src) {

			map.put(sec.getSourceNodeId(), sec);
		}

		return map;
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

		public AccessControllableTrait node = null;
		public BFSInfo parent               = null;
		public int level                    = 0;

		public BFSInfo(final BFSInfo parent, final AccessControllableTrait node) {

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
