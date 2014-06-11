package org.structr.core.parser;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */

public class IfExpression extends Expression {

	private Expression condition       = null;
	private Expression falseExpression = null;
	private Expression trueExpression  = null;

	public IfExpression() {
		super("if");
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression is the if condition
		if (this.condition == null) {

			this.condition = expression;

		} else if (this.trueExpression == null) {

			this.trueExpression = expression;

		} else if (this.falseExpression == null) {

			this.falseExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid if() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity) throws FrameworkException {


		if (condition == null) {
			return Functions.ERROR_MESSAGE_IF;
		}

		if (isTrue(condition.evaluate(securityContext, ctx, entity))) {

			if (trueExpression != null) {

				return trueExpression.evaluate(securityContext, ctx, entity);

			} else {

				throw new FrameworkException(422, "Invalid if() expression in builtin function: missing trueExpression.");
			}

		} else {

			if (falseExpression != null) {

				return falseExpression.evaluate(securityContext, ctx, entity);

			} else {

				throw new FrameworkException(422, "Invalid if() expression in builtin function: missing falseExpression.");
			}
		}
	}

	private boolean isTrue(final Object source) {
		return source != null && (Boolean.TRUE.equals(source) || "true".equals(source));
	}
}
