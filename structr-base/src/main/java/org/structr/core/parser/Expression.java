/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.parser;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public abstract class Expression implements Documentable {

	protected List<Expression> expressions = new LinkedList<>();
	protected Expression parent            = null;
	protected String name                  = null;
	protected int level                    = 0;
	protected int row                      = 1;
	protected int column                   = 1;

	public Expression(final int row, final int column) {
		this(null, row, column);
	}

	public Expression(final String name, final int row, final int column) {

		this.name   = name;
		this.row    = row;
		this.column = column;
	}

	public String getName() {
		return name;
	}

	public void add(final Expression expression) throws FrameworkException {

		expression.parent = this;
		expression.level  = this.level + 1;

		this.expressions.add(expression);
	}

	public void replacePrevious(final Expression newExpression) throws FrameworkException {

		if (hasPrevious()) {
			expressions.remove(expressions.size() - 1);
			this.add(newExpression);
		}
	}

	public List<Expression> getChildren() {
		return expressions;
	}

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.BuiltInFunction;
	}

	public Expression getParent() {
		return parent;
	}

	public Expression getPrevious() {

		if (!expressions.isEmpty()) {
			return expressions.get(expressions.size() - 1);
		}

		return null;
	}

	public boolean hasPrevious() {
		return !expressions.isEmpty();
	}

	public boolean isBatched() {
		return parent != null && parent.isBatched();
	}

	public int getBatchSize() {

		if (parent != null) {
			return parent.getBatchSize();
		}

		return -1;
	}

	public abstract Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException;
	public abstract Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException;
}
