/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.schema.action.ActionContext;

public class EscapeXmlFunction extends UiCommunityFunction {

	public static final String ERROR_MESSAGE_ESCAPE_XML    = "Usage: ${escape_xml(string)}. Example: ${escape_xml(\"test & test\")}";
	public static final String ERROR_MESSAGE_ESCAPE_XML_JS = "Usage: ${{Structr.escape_xml(string)}}. Example: ${{Structr.escape_xml(\"test & test\")}}";

	@Override
	public String getName() {
		return "escape_xml";
	}

	@Override
	public String getSignature() {
		return "string";
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ESCAPE_XML_JS : ERROR_MESSAGE_ESCAPE_XML);
	}

	@Override
	public String shortDescription() {
		return "Replaces XML characters with their corresponding XML entities";
	}
}
