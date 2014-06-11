package org.structr.core.parser;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class ValueExpression extends Expression {

	private String keyword = null;

	public ValueExpression(final String keyword) {
		this.keyword = keyword;
	}

	@Override
	public Object evaluate(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity) throws FrameworkException {
		return ctx.getReferencedProperty(securityContext, entity, keyword);
	}
}
