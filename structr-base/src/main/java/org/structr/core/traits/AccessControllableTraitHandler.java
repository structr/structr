package org.structr.core.traits;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

import java.util.List;
import java.util.Set;

public class AccessControllableTraitHandler extends AbstractTraitImplementation implements AccessControllableTrait {

	private final AccessControllableTraitImplementation defaultImplementation;

	public AccessControllableTraitHandler(final Traits traits) {

		super(traits);

		defaultImplementation = new AccessControllableTraitImplementation(traits);
	}

	@Override
	public PrincipalInterface getOwnerNode(NodeInterface node) {
		return defaultImplementation.getOwnerNode(node);
	}

	@Override
	public boolean isGranted(NodeInterface node, Permission permission, SecurityContext securityContext, boolean isCreation) {
		return defaultImplementation.isGranted(node, permission, securityContext, isCreation);
	}

	@Override
	public boolean allowedBySchema(NodeInterface node, PrincipalInterface principal, Permission permission) {
		return defaultImplementation.allowedBySchema(node, principal, permission);
	}

	@Override
	public void grant(NodeInterface node, Set<Permission> permissions, PrincipalInterface principal, SecurityContext ctx) throws FrameworkException {
		defaultImplementation.grant(node, permissions, principal, ctx);
	}

	@Override
	public void revoke(NodeInterface node, Set<Permission> permissions, PrincipalInterface principal, SecurityContext ctx) throws FrameworkException {
		defaultImplementation.revoke(node, permissions, principal, ctx);
	}

	@Override
	public void setAllowed(NodeInterface node, Set<Permission> permissions, PrincipalInterface principal, SecurityContext ctx) throws FrameworkException {
		defaultImplementation.setAllowed(node, permissions, principal, ctx);
	}

	@Override
	public List<RelationshipInterface<PrincipalInterface, NodeInterface>> getSecurityRelationships(NodeInterface node) {
		return defaultImplementation.getSecurityRelationships(node);
	}

	@Override
	public RelationshipInterface<PrincipalInterface, NodeInterface> getSecurityRelationship(NodeInterface node, PrincipalInterface principal) {
		return defaultImplementation.getSecurityRelationship(node, principal);
	}
}
