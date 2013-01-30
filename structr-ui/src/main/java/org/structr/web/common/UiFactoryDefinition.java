package org.structr.web.common;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.structr.common.DefaultFactoryDefinition;
import org.structr.common.RelType;
import org.structr.web.entity.DataNode;

/**
 *
 * @author Christian Morgner
 */
public class UiFactoryDefinition extends DefaultFactoryDefinition {
	
	@Override
	public String determineNodeType(Node node) {

		// instantiate nodes that have an incoming DEFINES_TYPE
		// relationship as a DataNode
		if (node.hasRelationship(Direction.INCOMING, RelType.DEFINES_TYPE)) {
			
			return DataNode.class.getSimpleName();
		}
		
		// else: default behaviour
		return super.determineNodeType(node);
	}
}
