package org.structr.core.entity.relationship;

import org.structr.core.Ownership;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class PrincipalOwnsNode extends OneToMany<Principal, NodeInterface> implements Ownership {

	@Override
	public Class<Principal> getSourceType() {
		return Principal.class;
	}

	@Override
	public String name() {
		return "OWNS";
	}

	@Override
	public Class<NodeInterface> getTargetType() {
		return NodeInterface.class;
	}
}
