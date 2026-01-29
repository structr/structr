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

import org.apache.commons.lang3.StringEscapeUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class EscapeJavascriptFunction extends CoreFunction {

	@Override
	public String getName() {
		return "escapeJavascript";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("string");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return StringEscapeUtils.escapeEcmaScript(sources[0].toString());

		} catch (ArgumentNullException ane) {

			// silently ignore null strings
			return null;

		} catch (ArgumentCountException ace) {

			logParameterError(caller, sources, ace.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${escapeJavascript(text)}. Example: ${escapeJavascript(this.name)}"),
			Usage.javaScript("Usage: ${{ $.escapeJavascript(text) }}. Example: ${{ $.escapeJavascript(this.name); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Escapes the given string for use with Javascript.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("text", "text to escape")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${escapeJavascript('This is a \"test\"')} => This is a \\\"test\\\" ")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Escapes the characters in a string using EcmaScript String rules.",
			"Escapes any values it finds into their EcmaScript String form.",
			"Deals correctly with quotes and control-chars (tab, backslash, cr, ff, etc.)."
		);
	}
}
