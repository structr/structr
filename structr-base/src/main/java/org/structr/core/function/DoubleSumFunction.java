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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class DoubleSumFunction extends CoreFunction {

	@Override
	public String getName() {
		return "doubleSum";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("list");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			double result = 0.0;

			if (sources[0] instanceof Iterable) {

				for (final Number num : (Iterable<Number>)sources[0]) {

					result += num.doubleValue();
				}
			}

			return result;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${doubleSum(list)}. Example: ${doubleSum(extract(this.children, 'amount'))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the sum of all the values in the given collection as a floating-point value.";
	}

	@Override
	public String getLongDescription() {
		return "This function will most likely be used in combination with the `extract()` or `merge()` functions.";
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("list", "list of values to sum")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${doubleSum(extract(find('Product'), 'itemPrice'))}", "Return the sum of all `itemPrice` values of all `Product` entities")
		);
	}
}
