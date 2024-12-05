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
package org.structr.core.traits.definitions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.PropagationDirection;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.accesscontrollable.*;
import org.structr.core.traits.operations.graphobject.*;

import java.util.*;
import java.util.stream.Collectors;

public final class AccessControllableTraitDefinition extends AbstractTraitDefinition {

	private static final Logger logger                                                                        = LoggerFactory.getLogger(AccessControllableTraitDefinition.class);
	private static final Map<String, Map<String, PermissionResolutionResult>> globalPermissionResolutionCache = new HashMap<>();
	private static final FixedSizeCache<String, Boolean> isGrantedResultCache                                 = new FixedSizeCache<>("Grant result cache", 100000);
	private static final int permissionResolutionMaxLevel                                                     = Settings.ResolutionDepth.getValue();

	public AccessControllableTraitDefinition() {
		super("AccessControllable");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of(

			OnModification.class,         new OnModificationActionVoid(AccessControllableTraitDefinition::clearCaches),
			OnDeletion.class,             new OnDeletionActionVoid(AccessControllableTraitDefinition::clearCaches),
			OwnerModified.class,          new OwnerModifiedActionVoid(AccessControllableTraitDefinition::clearCaches),
			SecurityModified.class,       new SecurityModifiedActionVoid(AccessControllableTraitDefinition::clearCaches),
			LocationModified.class,       new LocationModifiedActionVoid(AccessControllableTraitDefinition::clearCaches),
			PropagatedModification.class, new PropagatedModificationActionVoid(AccessControllableTraitDefinition::clearCaches)
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetOwnerNode.class,
			new GetOwnerNode() {

				@Override
				public Principal getOwnerNode(final NodeInterface nodeInterface) {

					final RelationshipInterface ownership = nodeInterface.getIncomingRelationshipAsSuperUser("PrincipalOwnsNode");
					if (ownership != null) {

						return ownership.getSourceNode().as(Principal.class);
					}

					return null;
				}
			},

			IsGranted.class,
			new IsGranted() {

				@Override
				public boolean isGranted(final NodeInterface node, final Permission permission, final SecurityContext securityContext, final boolean isCreation) {

					// super user can do everything
					if (securityContext != null && securityContext.isSuperUser()) {
						return true;
					}

					Principal accessingUser = null;
					if (securityContext != null) {

						accessingUser = securityContext.getUser(false);
					}

					final String cacheKey = node.getUuid() + "." + permission.name() + "." + securityContext.getCachedUserId();
					final Boolean cached = isGrantedResultCache.get(cacheKey);

					if (cached != null && cached == true) {
						return true;
					}

					final boolean doLog = node.getSecurityContext().hasParameter("logPermissionResolution");
					final boolean result = AccessControllableTraitDefinition.isGranted(node, permission, accessingUser, new PermissionResolutionMask(), 0, new AlreadyTraversed(), true, doLog, isCreation);

					isGrantedResultCache.put(cacheKey, result);

					return result;
				}
			},

			Grant.class,
			new Grant() {

				@Override
				public void grant(final NodeInterface node, final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException {

					if (!node.isGranted(Permission.accessControl, ctx, false)) {
						throw new FrameworkException(403, getAccessControlNotPermittedExceptionString(node, "grant", permissions, principal, ctx));
					}

					clearCaches();

					final Security secRel = node.getSecurityRelationship(principal);
					if (secRel == null) {

						try {

							final Set<String> permissionSet = new HashSet<>();

							for (Permission permission : permissions) {

								permissionSet.add(permission.name());
							}

							// ensureCardinality is not neccessary here
							final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
							final Traits traits                    = Traits.of("PrincipalOwnsNode");
							final PropertyMap properties           = new PropertyMap();

							superUserContext.disablePreventDuplicateRelationships();

							// performance improvement for grant(): add properties to the CREATE call that would
							// otherwise be set in separate calls later in the transaction.
							properties.put(traits.key("principalId"),         principal.getUuid());
							properties.put(traits.key("accessControllableId"), node.getUuid());
							properties.put(traits.key("allowed"),              permissionSet.toArray(new String[permissionSet.size()]));

							StructrApp.getInstance(superUserContext).create(principal.getWrappedNode(), node, "SecurityRelationship", properties);

						} catch (FrameworkException ex) {

							logger.error("Could not create security relationship!", ex);
						}

					} else {

						secRel.addPermissions(permissions);
					}
				}
			},

			Revoke.class,
			new Revoke() {

				@Override
				public void revoke(final NodeInterface node, final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException {

					if (!node.isGranted(Permission.accessControl, ctx, false)) {
						throw new FrameworkException(403, getAccessControlNotPermittedExceptionString(node, "revoke", permissions, principal, ctx));
					}

					clearCaches();

					final Security secRel = node.getSecurityRelationship(principal);
					if (secRel != null) {

						secRel.removePermissions(permissions);
					}
				}
			},

			SetAllowed.class,
			new SetAllowed() {

				@Override
				public void setAllowed(final NodeInterface node, final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException {

					if (!node.isGranted(Permission.accessControl, ctx, false)) {
						throw new FrameworkException(403, getAccessControlNotPermittedExceptionString(node, "set", permissions, principal, ctx));
					}

					clearCaches();

					final Set<String> permissionSet = new HashSet<>();

					for (Permission permission : permissions) {

						permissionSet.add(permission.name());
					}

					final Security secRel = node.getSecurityRelationship(principal);
					if (secRel == null) {

						if (!permissions.isEmpty()) {

							try {

								// ensureCardinality is not necessary here
								final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
								final Traits traits                    = Traits.of("PrincipalOwnsNode");
								final PropertyMap properties           = new PropertyMap();

								superUserContext.disablePreventDuplicateRelationships();

								// performance improvement for grant(): add properties to the CREATE call that would
								// otherwise be set in separate calls later in the transaction.
								properties.put(traits.key("principalId"),          principal.getUuid());
								properties.put(traits.key("accessControllableId"), node.getUuid());
								properties.put(traits.key("allowed"),              permissionSet.toArray(new String[permissionSet.size()]));

								StructrApp.getInstance(superUserContext).create(principal.getWrappedNode(), node, "SecurityRelationship", properties);

							} catch (FrameworkException ex) {

								logger.error("Could not create security relationship!", ex);
							}
						}

					} else {
						secRel.setAllowed(permissionSet);
					}
				}
			},

			GetSecurityRelationships.class,
			new GetSecurityRelationships() {

				@Override
				public List<Security> getSecurityRelationships(final NodeInterface node) {

					final List<RelationshipInterface> grants = Iterables.toList(node.getIncomingRelationshipsAsSuperUser("SecurityRelationship", null));
					final Traits traits                      = Traits.of("Security");

					// sort list by principal name (important for diff'able export)
					Collections.sort(grants, (o1, o2) -> {

						final NodeInterface p1 = o1.getSourceNode();
						final NodeInterface p2 = o2.getSourceNode();
						final String n1 = p1 != null ? p1.getProperty(Traits.nameProperty()) : "empty";
						final String n2 = p2 != null ? p2.getProperty(Traits.nameProperty()) : "empty";

						if (n1 != null && n2 != null) {
							return n1.compareTo(n2);

						} else if (n1 != null) {

							return 1;

						} else if (n2 != null) {
							return -1;
						}

						return 0;
					});

					// wrap in Security trait
					return grants.stream().map(g -> g.as(Security.class)).toList();
				}

				@Override
				public Security getSecurityRelationship(final NodeInterface node, final Principal p) {

					return AccessControllableTraitDefinition.getSecurityRelationship(
						p,
						AccessControllableTraitDefinition.mapSecurityRelationshipsMapped(node.getIncomingRelationshipsAsSuperUser("SecurityRelationship", null))
					);
				}
			},

			AllowedBySchema.class,
			new AllowedBySchema() {

				@Override
				public boolean allowedBySchema(final NodeInterface node, final Principal principal, final Permission permission) {
					return false;
				}
			}
		);
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of();
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	public static void clearCaches() {
		globalPermissionResolutionCache.clear();
		isGrantedResultCache.clear();
	}

	private static Security getSecurityRelationship(final Principal p, final Map<String, Security> securityRelationships) {

		if (p == null) {

			return null;
		}

		return securityRelationships.get(p.getUuid());
	}

	private static Map<String, Security> mapSecurityRelationshipsMapped(final Iterable<RelationshipInterface> src) {

		final Map<String, Security> map = new HashMap<>();

		for (final RelationshipInterface sec : src) {

			map.put(sec.getSourceNodeId(), sec.as(Security.class));
		}

		return map;
	}

	private static boolean isGranted(final NodeInterface node, final Permission permission, final Principal accessingUser, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final boolean resolvePermissions, final boolean doLog, final boolean isCreation) {
		return AccessControllableTraitDefinition.isGranted(node, permission, accessingUser, mask, level, alreadyTraversed, resolvePermissions, doLog, null, isCreation);
	}

	private static boolean isGranted(final NodeInterface node, final Permission permission, final Principal accessingUser, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final boolean resolvePermissions, final boolean doLog, final Map<String, Security> incomingSecurityRelationships, final boolean isCreation) {

		if (level > 300) {
			logger.warn("Aborting recursive permission resolution for {} on {} because of recursion level > 300, this is quite likely an infinite loop.", permission.name(), node.getType() + "(" + node.getUuid() + ")");
			return false;
		}

		if (doLog) { logger.info("{}{} ({}): {} check on level {} for {}", StringUtils.repeat("    ", level), node.getUuid(), node.getType(), permission.name(), level, accessingUser != null ? accessingUser.getName() : null); }

		if (accessingUser != null) {

			// this includes SuperUser
			if (accessingUser.isAdmin()) {
				return true;
			}

			// schema- (type-) based permissions
			if (node.allowedBySchema(accessingUser, permission)) {

				if (doLog) { logger.info("{}{} ({}): {} allowed on level {} by schema configuration for {}", StringUtils.repeat("    ", level), node.getUuid(), node.getType(), permission.name(), level, accessingUser != null ? accessingUser.getName() : null); }
				return true;
			}
		}

		final Principal _owner = node.getOwnerNode();
		final boolean hasOwner          = (_owner != null);

		if (isCreation && (accessingUser == null || accessingUser.equals(node) || accessingUser.equals(_owner) ) ) {
			return true;
		}

		// allow accessingUser to access itself, but not parents etc.
		if (node.equals(accessingUser) && (level == 0 || (permission.equals(Permission.read) && level > 0))) {
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

			final Map<String, Security> localIncomingSecurityRelationships = incomingSecurityRelationships != null ? incomingSecurityRelationships : mapSecurityRelationshipsMapped(node.getIncomingRelationshipsAsSuperUser("SecurityRelationship", null));
			final Security security                                        = AccessControllableTraitDefinition.getSecurityRelationship(accessingUser, localIncomingSecurityRelationships);

			if (security != null && security.isAllowed(permission)) {
				if (doLog) { logger.info("{}{} ({}): {} allowed on level {} by security relationship for {}", StringUtils.repeat("    ", level), node.getUuid(), node.getType(), permission.name(), level, accessingUser != null ? accessingUser.getName() : null); }
				return true;
			}

			for (Principal parent : accessingUser.getParentsPrivileged()) {

				if (isGranted(node, permission, parent, mask, level+1, alreadyTraversed, false, doLog, localIncomingSecurityRelationships, isCreation)) {
					return true;
				}
			}

			// Check permissions from domain relationships
			if (resolvePermissions) {

				final Queue<BFSInfo> bfsNodes   = new LinkedList<>();
				final BFSInfo root              = new BFSInfo(null, node);

				// add initial element
				bfsNodes.add(root);

				do {

					final BFSInfo info = bfsNodes.poll();
					if (info != null && info.level < permissionResolutionMaxLevel) {

						final Boolean value = getPermissionResolutionResult(info.node, accessingUser.getUuid(), permission);
						if (value != null) {

							// returning immediately
							if (Boolean.TRUE.equals(value)) {

								// do backtracking
								backtrack(info, accessingUser.getUuid(), permission, true, 0, doLog);

								return true;
							}

						} else {

							if (hasEffectivePermissions(info.node, info, accessingUser, permission, mask, level, alreadyTraversed, bfsNodes, doLog, isCreation)) {

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

	private static void backtrack(final BFSInfo info, final String principalId, final Permission permission, final boolean value, final int level, final boolean doLog) {

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

		storePermissionResolutionResult(info.node, principalId, permission, value);

		// go to parent(s)
		if (info.parent != null) {

			backtrack(info.parent, principalId, permission, value, level+1, doLog);
		}

		if (doLog && level == 0) {
			logger.info(buf.toString());
		}
	}

	private static boolean hasEffectivePermissions(final NodeInterface node, final BFSInfo parent, final Principal principal, final Permission permission, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final Queue<BFSInfo> bfsNodes, final boolean doLog, final boolean isCreation) {

		// check nodes here to avoid circles in permission-propagating relationships
		if (alreadyTraversed.contains("Node", node.getUuid())) {
			return false;
		}

		if (doLog) { logger.info("{}{} ({}): checking {} access on level {} for {}", StringUtils.repeat("    ", level), node.getUuid(), node.getType(), permission.name(), level, principal != null ? principal.getName() : null); }

		final Node dbNode              = node.getNode();
		final Map<String, Long> degree = dbNode.getDegree();

		for (final String type : degree.keySet()) {

			/*
			final Class propagatingType = StructrApp.getConfiguration().getRelationshipEntityClass(type);

			if (propagatingType != null && PermissionPropagation.class.isAssignableFrom(propagatingType)) {

				// iterate over list of relationships
				final List<RelationshipInterface> list = Iterables.toList(node.getRelationshipsAsSuperUser(propagatingType));
				final int count = list.size();
				final int threshold = 1000;

				if (count < threshold) {

					for (final RelationshipInterface source : list) {

						if (source instanceof PermissionPropagation perm) {

							if (doLog) {
								logger.info("{}{}: checking {} access on level {} via {} for {}", StringUtils.repeat("    ", level), node.getUuid(), permission.name(), level, source.getRelType().name(), principal != null ? principal.getName() : null);
							}

							// check propagation direction vs. evaluation direction
							if (propagationAllowed(node, source, perm.getPropagationDirection(), doLog)) {

								applyCurrentStep(perm, mask);

								if (mask.allowsPermission(permission)) {

									final NodeInterface otherNode = (NodeInterface) source.getOtherNode(node);

									if (isGranted(otherNode, permission, principal, mask, level + 1, alreadyTraversed, false, doLog, isCreation)) {

										storePermissionResolutionResult(otherNode, principal.getUuid(), permission, true);

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
			*/
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
	private static boolean propagationAllowed(final NodeInterface thisNode, final RelationshipInterface rel, final PropagationDirection propagationDirection, final boolean doLog) {

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

	private static void applyCurrentStep(final PermissionPropagation rel, PermissionResolutionMask mask) {

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

	private static Boolean getPermissionResolutionResult(final NodeInterface node, final String principalId, final Permission permission) {

		Map<String, PermissionResolutionResult> permissionResolutionCache = globalPermissionResolutionCache.get(node.getUuid());
		if (permissionResolutionCache == null) {

			permissionResolutionCache = new HashMap<>();
			globalPermissionResolutionCache.put(node.getUuid(), permissionResolutionCache);
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

	private static void storePermissionResolutionResult(final NodeInterface node, final String principalId, final Permission permission, final boolean value) {

		Map<String, PermissionResolutionResult> permissionResolutionCache = globalPermissionResolutionCache.get(node.getUuid());
		if (permissionResolutionCache == null) {

			permissionResolutionCache = new HashMap<>();
			globalPermissionResolutionCache.put(node.getUuid(), permissionResolutionCache);
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

	protected String getAccessControlNotPermittedExceptionString(final GraphObject graphObject, final String action, final Set<Permission> permissions, Principal principal, final SecurityContext ctx) {

		final String userString       = PropertyContainerTraitDefinition.getCurrentUserString(ctx);
		final String thisNodeString   = graphObject.getType()      + "(" + graphObject.getUuid()      + ")";
		final String principalString  = principal.getType() + "(" + principal.getUuid() + ")";
		final String permissionString = permissions.stream().map(p -> p.name()).collect(Collectors.joining(", "));

		return "Access control not permitted! " + userString + " can not " + action + " rights (" + permissionString + ") for " + principalString + " to node " + thisNodeString;
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

		public NodeInterface node = null;
		public BFSInfo parent     = null;
		public int level          = 0;

		public BFSInfo(final BFSInfo parent, final NodeInterface node) {

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
