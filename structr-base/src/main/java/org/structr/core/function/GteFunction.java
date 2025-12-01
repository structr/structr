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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class GteFunction extends CoreFunction {

	@Override
	public String getName() {
		return "gte";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value1, value2");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		return gte(sources[0], sources[1]);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${gte(value1, value2)}. Example: ${if(gte(this.children, 2), 'Equal to or more than two', 'Less than two')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns true if the first argument is greater than or equal to the second argument.";
	}

	@Override
	public String getLongDescription() {
		return "This method tries to convert its arguments into numerical values, i.e. you can compare strings numerically.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("value1", "first value"),
			Parameter.mandatory("value2", "second value")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript(" ${gte(1, 2)} ", "This will return `false`"),
			Example.structrScript(" ${gte(2, 1)} ", "This will return `true`"),
			Example.structrScript(" ${gte(2, 2)} ", "This will return `true`")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
