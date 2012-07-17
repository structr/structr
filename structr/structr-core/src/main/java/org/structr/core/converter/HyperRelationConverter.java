/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.converter;

import java.util.Collections;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */
public class HyperRelationConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {
		
		// read only
		return null;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		if(currentObject != null && value != null) {
			
			Object parameterObject = value.get(securityContext);
			
			if(parameterObject instanceof HyperRelation) {

				HyperRelation hyperRelation   = (HyperRelation)parameterObject;
				AbstractNode parent           = (AbstractNode)currentObject;
				Notion notion                 = hyperRelation.getNotion();
				
				// create temporary relation class
				RelationClass parentRelation  = new RelationClass(
									hyperRelation.getEntity(),
									hyperRelation.getRelType(),
									hyperRelation.getDirection(),
									RelationClass.Cardinality.ManyToMany,
									notion,
									0
								);

				return parentRelation.getRelatedNodes(securityContext, parent);
			}
			
		}
		
		return Collections.emptyList();
	}
	
}
