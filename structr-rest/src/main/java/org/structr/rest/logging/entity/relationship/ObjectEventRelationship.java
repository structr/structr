package org.structr.rest.logging.entity.relationship;

import org.structr.core.entity.ManyToOne;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.rest.logging.entity.LogEvent;

/**
 *
 * @author Christian Morgner
 */
public class ObjectEventRelationship extends ManyToOne<LogEvent, NodeInterface> {

	@Override
	public Class<NodeInterface> getTargetType() {
		return NodeInterface.class;
	}

	@Override
	public Class<LogEvent> getSourceType() {
		return LogEvent.class;
	}

	@Override
	public String name() {
		return "OBJECT";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
