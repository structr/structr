package org.structr.core.parser;

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
	public Object evaluate(ActionContext ctx, GraphObject entity) throws FrameworkException {
		return ctx.getReferencedProperty(entity.getSecurityContext(), entity, keyword);
	}
}
