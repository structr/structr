/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.Map;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.rest.exception.NotFoundException;

/**
 * Represents a type-constrained ID match with match on all sub-types. It will always
 * result in a single element.
 * 
 * @author Christian Morgner
 */
public class InheritingTypedIdConstraint extends TypedIdConstraint {

	private static final Logger logger = Logger.getLogger(InheritingTypedIdConstraint.class.getName());

	protected InheritingTypedIdConstraint(SecurityContext securityContext) {
		super(securityContext);
	}

	public InheritingTypedIdConstraint(SecurityContext securityContext, IdConstraint idConstraint, InheritingTypeConstraint typeConstraint) {
		super(securityContext, idConstraint, typeConstraint);
	}

	@Override
	public AbstractNode getTypesafeNode() throws FrameworkException {
		
		AbstractNode node = idConstraint.getNode();

		Class entityClass = node.getClass();
		String type = EntityContext.normalizeEntityName(typeConstraint.getRawType());

		Map<String, Class> entities = (Map) Services.command(SecurityContext.getSuperUserInstance(), GetEntitiesCommand.class).execute();
		Class parentClass           = entities.get(type);

		if (parentClass.isAssignableFrom(entityClass)) {
			return node;
		}
		
		throw new NotFoundException();
	}
	
	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws FrameworkException {

		if(next instanceof TypeConstraint) {

			// next constraint is a type constraint
			// => follow predefined statc relationship
			//    between the two types
			return new StaticRelationshipConstraint(securityContext, this, (TypeConstraint)next);

		} else if(next instanceof InheritingTypedIdConstraint) {

			RelationshipFollowingConstraint constraint = new RelationshipFollowingConstraint(securityContext, this);
			constraint.addTypedIdConstraint((InheritingTypedIdConstraint)next);

			return constraint;

		} else if(next instanceof RelationshipConstraint) {

			// make rel constraint wrap this
			((RelationshipConstraint)next).wrapConstraint(this);
			return next;

		} else if(next instanceof RelationshipIdConstraint) {

			((RelationshipIdConstraint)next).getRelationshipConstraint().wrapConstraint(this);
			return next;
		}

		return super.tryCombineWith(next);
	}

}
