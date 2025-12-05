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

public class LtFunction extends CoreFunction {


	@Override
	public String getName() {
		return "lt";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value1, value2");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		return lt(sources[0], sources[1]);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${lt(value1, value2)}. Example: ${if(lt(size(this.children), 2), 'Less than two', 'Equal to or more than two')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns true if the first argument is less than the second argument.";
	}

	@Override
	public String getLongDescription() {
		return "This function tries to convert its parameter objects into numerical values, i.e. you can compare strings numerically.";
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
			Example.structrScript(" ${lt(1, 2)} ", "This will return `true`"),
			Example.structrScript(" ${lt(2, 1)} ", "This will return `false`")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
