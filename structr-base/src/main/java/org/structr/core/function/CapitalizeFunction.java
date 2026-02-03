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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class CapitalizeFunction extends CoreFunction {

	@Override
	public String getName() {
		return "capitalize";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return StringUtils.capitalize(sources[0].toString());

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
			Usage.javaScript("Usage: ${{$.capitalize(string)}}. Example: ${{$.capitalize($.this.nickName)}}"),
			Usage.structrScript("Usage: ${capitalize(string)}. Example: ${capitalize(this.nickName)}")
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("string");
	}

	@Override
	public String getShortDescription() {
		return "Capitalizes the given string.";
	}

	@Override
	public String getLongDescription() {
		return "No other characters are changed. If the first character has no explicit titlecase mapping and is not itself a titlecase char according to UnicodeData, then the uppercase mapping is returned as an equivalent titlecase mapping.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${capitalize('cat dog bird')}", "Results in \"Cat dog bird\""),
			Example.structrScript("${capitalize('cAT DOG BIRD')}", "Results in \"CAT DOG BIRD\""),
			Example.structrScript("${capitalize('\"cat dog bird\"')}", "Only the first character is capitalized, so quoted strings are not changed")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.String;
	}
}
