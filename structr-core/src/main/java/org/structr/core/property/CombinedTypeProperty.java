package org.structr.core.property;

import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author Christian Morgner
 */
public class CombinedTypeProperty extends AutoStringProperty {

	public CombinedTypeProperty() {
		
		super("combinedType");
	}
	
	@Override
	public String createValue(GraphObject entity) {
		
		if (entity instanceof AbstractRelationship) {

			AbstractRelationship rel = (AbstractRelationship)entity;
			AbstractNode startNode   = rel.getStartNode();
			AbstractNode endNode     = rel.getEndNode();
			
			return EntityContext.createCombinedRelationshipType(startNode.getType(), rel.getType(), endNode.getType());
		}
	
		return null;
	}
}
