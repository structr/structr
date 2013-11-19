package org.structr.core.entity.relationship;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.OneToMany;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

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
	public SourceId getSourceIdProperty() {
		return null;
	}

	@Override
	public TargetId getTargetIdProperty() {
		return null;
	}

}
