package org.structr.core.parser;

import java.util.ArrayList;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 * @author Christian Morgner
 */
public class FunctionExpression extends Expression {

	private Function<Object, Object> function = null;

	public FunctionExpression(final String name, final Function<Object, Object> function) {

		super(name);

		this.function = function;
	}

	@Override
	public Object evaluate(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity) throws FrameworkException {

		final ArrayList<Object> results = new ArrayList<>();
		for (Expression expr : expressions) {

			final Object result = expr.evaluate(securityContext, ctx, entity);
			results.add(result);
		}

		if (results.isEmpty()) {
			return function.usage();
		}

		return function.apply(ctx, entity, results.toArray());
	}
}
