package org.structr.core.parser;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */

public class FilterExpression extends Expression {

	private Expression listExpression   = null;
	private Expression filterExpression = null;

	public FilterExpression() {
		super("filter");
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression must yield a List
		if (this.listExpression == null) {

			this.listExpression = expression;

		} else if (this.filterExpression == null) {

			this.filterExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid filter() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity) throws FrameworkException {

		if (listExpression == null) {
			return Functions.ERROR_MESSAGE_FILTER;
		}

		final Object listSource = listExpression.evaluate(securityContext, ctx, entity);
		final List target       = new LinkedList<>();

		if (listSource != null && listSource instanceof List) {

			final List source = (List)listSource;

			for (Object obj : source) {

				final Object result = filterExpression.evaluate(securityContext, new ActionContext(entity, obj), entity);
				if (result instanceof Boolean) {

					if ((Boolean)result) {

						target.add(obj);
					}
				}
			}
		}

		return target;
	}
}
