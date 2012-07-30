/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.converter;

import java.util.Collections;
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
