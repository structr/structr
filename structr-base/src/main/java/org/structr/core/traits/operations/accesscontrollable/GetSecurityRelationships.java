package org.structr.core.traits.operations.accesscontrollable;

import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.traits.operations.OverwritableOperation;

import java.util.List;

public abstract class GetSecurityRelationships extends OverwritableOperation<GetSecurityRelationships> {

	public abstract List<RelationshipInterface<Principal, NodeInterface>> getSecurityRelationships(final NodeInterface node);
	public abstract RelationshipInterface<Principal, NodeInterface> getSecurityRelationship(final NodeInterface node, final Principal principal);
}
