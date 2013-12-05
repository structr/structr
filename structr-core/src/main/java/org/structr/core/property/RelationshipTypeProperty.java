package org.structr.core.property;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.graph.RelationshipInterface;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipTypeProperty extends AbstractReadOnlyProperty {

	public RelationshipTypeProperty(final String name) {
		super(name);
	}
	
	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public Object getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		if (obj instanceof RelationshipInterface) {
			
			return ((RelationshipInterface)obj).getRelType().name();
		}
		
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Integer getSortType() {
		return null;
	}
}
