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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Language;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MaxFunction extends CoreFunction {


	@Override
	public String getName() {
		return "max";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value1, value2");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			return Math.max(Double.parseDouble(sources[0].toString()), Double.parseDouble(sources[1].toString()));

		} catch (NumberFormatException nfe) {

			logException(nfe, "{}: NumberFormatException in element \"{}\" for parameters: {}", new Object[] { getDisplayName(), caller, getParametersAsString(sources) });
			return usage(ctx.isJavaScriptContext());

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
			Usage.structrScript("Usage: ${max(value1, value2)}. Example: ${max(this.children, 10)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the greater of the given values.";
	}

	@Override
	public String getLongDescription() {
		return "This method tries to convert its arguments into numerical values, i.e. you can use strings as arguments. See also `min()`.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("value1", "first value to compare"),
			Parameter.mandatory("value2", "second value to compare")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
