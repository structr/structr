package org.structr.rest.logging.entity;

import java.util.List;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.rest.logging.entity.relationship.SubjectEventRelationship;

/**
 *
 * @author Christian Morgner
 */
public class LogSubject extends AbstractNode {

	public static final Property<List<LogEvent>> logEvents = new EndNodes<>("logEvents", SubjectEventRelationship.class);
}
