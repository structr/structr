/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.converter;

import org.structr.core.PropertyConverter;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipEndNodeTypeConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {
		return null;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		
		if(currentObject instanceof AbstractRelationship) {
			
			AbstractRelationship rel = (AbstractRelationship)currentObject;
			if(rel != null) {
				
				AbstractNode endNode = rel.getEndNode();
				if(endNode != null) {
					
					return endNode.getType();
				}
			}
		}
		
		return null;
	}
}
