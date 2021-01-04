/*
 * Copyright (C) 2010-2021 Structr GmbH
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
import org.structr.schema.action.ActionContext;

import java.util.function.Supplier;

public class LazyEvaluatedFunctionExpression extends Expression {
	private final Supplier supplier;

	public LazyEvaluatedFunctionExpression(final Supplier supplier) {
		this.supplier = supplier;
	}

	@Override
	public Object evaluate(ActionContext ctx, GraphObject entity) throws FrameworkException, UnlicensedScriptException {
		return this.supplier.get();
	}

	@Override
	public Object transform(ActionContext ctx, GraphObject entity, Object source) throws FrameworkException, UnlicensedScriptException {
		return source;
	}
}
