package org.structr.core.parser;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class RootExpression extends Expression {

	@Override
	public Object evaluate(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity) throws FrameworkException {

		if (!expressions.isEmpty()) {
			return expressions.get(0).evaluate(securityContext, ctx, entity);
		}

		return null;
	}
}
