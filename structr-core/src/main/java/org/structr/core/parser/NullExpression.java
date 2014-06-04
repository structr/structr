package org.structr.core.parser;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class NullExpression extends Expression {

	@Override
	public Object evaluate(ActionContext ctx, GraphObject entity) throws FrameworkException {
		return null;
	}
}
