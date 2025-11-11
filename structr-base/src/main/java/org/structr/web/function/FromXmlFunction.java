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
package org.structr.web.function;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FromXmlFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "from_xml";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("source");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		if (sources != null && sources.length > 0) {

			if (sources[0] == null) {
				return "";
			}

			try {

				return org.json.XML.toJSONObject(sources[0].toString()).toString(4);

			} catch (Throwable t) {

				logException(caller, t, sources);
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${from_xml(source)}. Example: ${from_xml('<entry>0</entry>')}"),
			Usage.javaScript("Usage: ${{Structr.fromXml(src)}}. Example: ${{Structr.fromXml('<entry>0</entry>')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Parses the given XML and returns a JSON representation of the XML";
	}
}
