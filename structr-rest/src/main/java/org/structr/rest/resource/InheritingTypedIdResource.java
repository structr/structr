/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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

package org.structr.rest.resource;

import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.rest.exception.NotFoundException;

/**
 * Represents a type-constrained ID match with match on all sub-types. It will always
 * result in a single element.
 * 
 * @author Christian Morgner
 */
public class InheritingTypedIdResource extends TypedIdResource {

	private static final Logger logger = Logger.getLogger(InheritingTypedIdResource.class.getName());

	protected InheritingTypedIdResource(SecurityContext securityContext) {
		super(securityContext);
	}

	public InheritingTypedIdResource(SecurityContext securityContext, UuidResource idResource, InheritingTypeResource typeResource) {
		super(securityContext, idResource, typeResource);
	}

	@Override
	public AbstractNode getTypesafeNode() throws FrameworkException {
		
		AbstractNode node = idResource.getNode();

		String type       = EntityContext.normalizeEntityName(typeResource.getRawType());
		Class parentClass = EntityContext.getEntityClassForRawType(type);
		Class entityClass = node.getClass();

		if (parentClass.isAssignableFrom(entityClass)) {
			return node;
		}
		
		throw new NotFoundException();
	}
	
	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if(next instanceof TypeResource) {

			// next constraint is a type constraint
			// => follow predefined statc relationship
			//    between the two types
			return new StaticRelationshipResource(securityContext, this, (TypeResource)next);

		} else if(next instanceof InheritingTypedIdResource) {

			RelationshipFollowingResource constraint = new RelationshipFollowingResource(securityContext, this);
			constraint.addTypedIdResource((InheritingTypedIdResource)next);

			return constraint;

		} else if(next instanceof RelationshipResource) {

			// make rel constraint wrap this
			((RelationshipResource)next).wrapResource(this);
			return next;

		} else if(next instanceof RelationshipIdResource) {

			((RelationshipIdResource)next).getRelationshipResource().wrapResource(this);
			return next;
		}

		return super.tryCombineWith(next);
	}

}
