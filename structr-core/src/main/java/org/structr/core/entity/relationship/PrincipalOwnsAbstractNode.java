package org.structr.core.entity.relationship;

import org.structr.core.Ownership;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Principal;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

/**
 *
 * @author Christian Morgner
 */
public class PrincipalOwnsAbstractNode extends OneToMany<Principal, AbstractNode> implements Ownership {

	@Override
	public Class<Principal> getSourceType() {
		return Principal.class;
	}

	@Override
	public String name() {
		return "OWNS";
	}

	@Override
	public Class<AbstractNode> getTargetType() {
		return AbstractNode.class;
	}

	@Override
	public SourceId getSourceIdProperty() {
		return null;
	}

	@Override
	public TargetId getTargetIdProperty() {
		return null;
	}
}
