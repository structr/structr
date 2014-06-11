package org.structr.core.parser;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */

public class GroupExpression extends Expression {

	@Override
	public Object evaluate(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity) throws FrameworkException {

		final StringBuilder buf = new StringBuilder();
		for (Expression expr : expressions) {

			final Object result = expr.evaluate(securityContext, ctx, entity);
			if (result != null) {

				buf.append(result);
			}
		}

		return buf.toString();
	}
}
