package org.structr.core.traits;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.graph.Node;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.common.Permission;
import org.structr.common.PermissionPropagation;
import org.structr.common.PermissionResolutionMask;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.entity.Security;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

import java.util.*;

public class AccessControllableTraitImplementation extends AbstractTraitImplementation implements AccessControllableTrait {

	private static final FixedSizeCache<String, Boolean> isGrantedResultCache = new FixedSizeCache<>("Grant result cache", 100000);
	protected PrincipalInterface cachedOwnerNode = null;

	public AccessControllableTraitImplementation(final Traits traits) {
		super(traits);
	}

	@Override
	public boolean isGranted(final NodeInterface node, final Permission permission, final SecurityContext context, final boolean isCreation) {

		// super user can do everything
		if (context != null && context.isSuperUser()) {
			return true;
		}

		PrincipalInterface accessingUser = null;
		if (context != null) {

			accessingUser = context.getUser(false);
		}

		final String cacheKey = node.getUuid() + "." + permission.name() + "." + context.getCachedUserId();
		final Boolean cached  = isGrantedResultCache.get(cacheKey);

		if (cached != null && cached == true) {
			return true;
		}

		final boolean doLog  = node.getSecurityContext().hasParameter("logPermissionResolution");
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


	private boolean hasEffectivePermissions(final GraphObject obj, final BFSInfo parent, final PrincipalInterface principal, final Permission permission, final PermissionResolutionMask mask, final int level, final AlreadyTraversed alreadyTraversed, final Queue<BFSInfo> bfsNodes, final boolean doLog, final boolean isCreation) {

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

	public PrincipalInterface getOwnerNode(final NodeInterface node) {

		if (cachedOwnerNode == null) {

			final RelationshipInterface<PrincipalInterface, NodeInterface> ownership = getIncomingRelationshipAsSuperUser(PrincipalOwnsNode.class);
			if (ownership != null) {

				cachedOwnerNode = ownership.getSourceNode();
			}
		}

		return cachedOwnerNode;
	}

	@Override
	public boolean isGranted(final NodeInterface node, final Permission permission, final SecurityContext securityContext) {
		return isGranted(node, permission, securityContext, false);
	}

	@Override
	public void grant(final NodeInterface node, final Permission permission, final PrincipalInterface principal) throws FrameworkException {
	}

	@Override
	public void grant(final NodeInterface node, final Set<Permission> permissions, final PrincipalInterface principal) throws FrameworkException {
	}

	@Override
	public void grant(final NodeInterface node, final Set<Permission> permissions, final PrincipalInterface principal, final SecurityContext ctx) throws FrameworkException {
	}

	@Override
	public void revoke(final NodeInterface node, final Permission permission, final PrincipalInterface principal) throws FrameworkException {
	}

	@Override
	public void revoke(final NodeInterface node, final Set<Permission> permissions, final PrincipalInterface principal) throws FrameworkException {
	}

	@Override
	public void revoke(final NodeInterface node, final Set<Permission> permissions, final PrincipalInterface principal, final SecurityContext ctx) throws FrameworkException {
	}

	@Override
	public void setAllowed(final NodeInterface node, final Set<Permission> permissions, final PrincipalInterface principal) throws FrameworkException {
	}

	@Override
	public void setAllowed(final NodeInterface node, final Set<Permission> permissions, final PrincipalInterface principal, final SecurityContext ctx) throws FrameworkException {
	}

	// visibility
	@Override
	public boolean isVisibleToPublicUsers(final NodeInterface node) {
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers(final NodeInterface node) {
	}

	@Override
	public boolean isHidden(final NodeInterface node) {
	}

	// access
	@Override
	public Date getCreatedDate(final NodeInterface node) {
	}

	@Override
	public Date getLastModifiedDate(final NodeInterface node) {
	}

	@Override
	public List<RelationshipInterface<PrincipalInterface, NodeInterface>> getSecurityRelationships(final NodeInterface node) {

		final List<RelationshipInterface<PrincipalInterface, NodeInterface>> grants = Iterables.toList(node.getIncomingRelationshipsAsSuperUser(Security.class, null));

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
	public RelationshipInterface<PrincipalInterface, NodeInterface> getSecurityRelationship(NodeInterface node, PrincipalInterface p) {

		if (p != null) {

			// try filter predicate to speed up things
			final Predicate<NodeInterface> principalFilter = (NodeInterface value) -> {
				return (p.getUuid().equals(value.getUuid()));
			};

			return getSecurityRelationship(p, mapSecurityRelationshipsMapped(node.getIncomingRelationshipsAsSuperUser(Security.class, principalFilter)));
		}

		return getSecurityRelationship(p, mapSecurityRelationshipsMapped(node.getIncomingRelationshipsAsSuperUser(Security.class, null)));
	}

	private RelationshipInterface<PrincipalInterface, NodeInterface> getSecurityRelationship(final PrincipalInterface p, final Map<String, RelationshipInterface<PrincipalInterface, NodeInterface>> securityRelationships) {

		if (p == null) {

			return null;
		}

		return securityRelationships.get(p.getUuid());
	}

	private Map<String, RelationshipInterface<PrincipalInterface, NodeInterface>> mapSecurityRelationshipsMapped(final Iterable<RelationshipInterface<PrincipalInterface, NodeInterface>> src) {

		final Map<String, RelationshipInterface<PrincipalInterface, NodeInterface>> map = new HashMap<>();

		for (final RelationshipInterface<PrincipalInterface, NodeInterface> sec : src) {

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
