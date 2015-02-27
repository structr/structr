package org.structr.rest.logging.entity.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.rest.logging.entity.LogEvent;

/**
 *
 * @author Christian Morgner
 */
public class SubjectEventRelationship extends OneToMany<NodeInterface, LogEvent> {

	@Override
	public Class<NodeInterface> getSourceType() {
		return NodeInterface.class;
	}

	@Override
	public Class<LogEvent> getTargetType() {
		return LogEvent.class;
	}

	@Override
	public String name() {
		return "SUBJECT";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.TARGET_TO_SOURCE;
	}
}
