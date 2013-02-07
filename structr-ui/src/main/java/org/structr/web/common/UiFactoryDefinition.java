package org.structr.web.common;

import org.neo4j.graphdb.Node;
import org.structr.common.DefaultFactoryDefinition;
import org.structr.web.entity.DataNode;
import org.structr.web.experimental.DataNodeExtender;

/**
 *
 * @author Christian Morgner
 */
public class UiFactoryDefinition extends DefaultFactoryDefinition {
	
	private static final String DATA_NODE_TYPE     = DataNode.class.getSimpleName();
	private static final String KIND               = DataNode.kind.dbName();

	public static final DataNodeExtender extender = new DataNodeExtender();
	
	@Override
	public String determineNodeType(Node node) {

		String nodeType = super.determineNodeType(node);
		
		if (DATA_NODE_TYPE.equals(nodeType) && node.hasProperty(KIND)) {
			
			String kind = (String)node.getProperty(KIND);
			
			// initialize type
			extender.getType(kind);
			
			// return dynamic kind instead of type
			nodeType = kind;
		}
		
		return nodeType;
	}
}
