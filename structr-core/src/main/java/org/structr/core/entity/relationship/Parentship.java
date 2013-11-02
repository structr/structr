package org.structr.core.entity.relationship;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.OneToMany;

/**
 *
 * @author Christian Morgner
 */
public class Parentship extends OneToMany<AbstractNode, AbstractNode> {

	@Override
	public Class<AbstractNode> getSourceType() {
		return AbstractNode.class;
	}

	@Override
	public String name() {
		return "CONTAINS";
	}

	@Override
	public Class<AbstractNode> getTargetType() {
		return AbstractNode.class;
	}

	@Override
	public Class<? extends AbstractRelationship<AbstractNode, AbstractNode>> reverse() {
		return Parentship.class;
	}

}
