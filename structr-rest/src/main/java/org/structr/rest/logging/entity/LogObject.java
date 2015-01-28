package org.structr.rest.logging.entity;

import java.util.List;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.rest.logging.entity.relationship.ObjectEventRelationship;

/**
 *
 * @author Christian Morgner
 */
public class LogObject extends AbstractNode {

	public static final Property<List<LogEvent>> logEvents = new StartNodes<>("logEvents", ObjectEventRelationship.class);
}
