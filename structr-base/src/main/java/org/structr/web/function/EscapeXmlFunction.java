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
package org.structr.web.function;

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

public class EscapeXmlFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "escapeXml";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("string");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return StringEscapeUtils.escapeXml(sources[0].toString());

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
			Usage.structrScript("Usage: ${escapeXml(text)}. Example: ${escapeXml('test & test')}"),
			Usage.javaScript("Usage: ${{ $.escapeXml(text)}}. Example: ${{ $.escapeXml('test & test')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Replaces XML characters with their corresponding XML entities.";
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
			Example.structrScript("${escapeXml('This is a \"test\" & another \"test\"')} => This is a &quot;test&quot; &amp; another &quot;test&quot;")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Supports only the five basic XML entities (gt, lt, quot, amp, apos).",
			"Does not support DTDs or external entities.",
			"Unicode characters greater than 0x7f are currently escaped to their numerical \\u equivalent. This may change in future releases."
		);
	}
}
