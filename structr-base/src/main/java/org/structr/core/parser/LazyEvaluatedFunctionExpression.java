/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.docs.*;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.List;
import java.util.function.Supplier;

public class LazyEvaluatedFunctionExpression extends Expression {

	private final Supplier supplier;

	public LazyEvaluatedFunctionExpression(final Supplier supplier, final int row, final int column) {

		super(row, column);
		
		this.supplier = supplier;
	}

	@Override
	public Object evaluate(ActionContext ctx, GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return this.supplier.get();
	}

	@Override
	public Object transform(ActionContext ctx, GraphObject entity, Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
	}

	// ----- documentation (unused) -----
	@Override
	public String getShortDescription() {
		return "";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of();
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of();
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of();
	}

	@Override
	public List<Language> getLanguages() {
		return List.of();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of();
	}

	@Override
	public FunctionCategory getCategory() {
		return null;
	}
}
