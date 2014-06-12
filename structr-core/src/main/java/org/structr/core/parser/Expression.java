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
public abstract class Expression {

	protected List<Expression> expressions = new LinkedList<>();
	protected Expression parent            = null;
	protected String name                  = null;
	protected int level                    = 0;

	public Expression() {
		this(null);
	}

	public Expression(final String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public void add(final Expression expression) throws FrameworkException {

		expression.parent = this;
		expression.level  = this.level + 1;

		this.expressions.add(expression);
	}

	public Expression getParent() {
		return parent;
	}

	public abstract Object evaluate(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity) throws FrameworkException;
}
