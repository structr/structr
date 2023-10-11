/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.schema.action.ActionContext;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import javax.xml.xpath.*;

public class XPathFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_XPATH = "Usage: ${xpath(xmlDocument, expression, returnType)}. Example: ${xpath(xml(this.xmlSource), \"/test/testValue\" [, \"STRING\"])}";

	@Override
	public String getName() {
		return "xpath";
	}

	@Override
	public String getSignature() {
		return "document, xpath [, returnType ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof Document) {

				try {

					final XPath xpath = XPathFactory.newInstance().newXPath();
					QName returnType  = XPathConstants.STRING;

					if (sources.length == 3 && sources[2] instanceof String) {

						returnType = new QName("http://www.w3.org/1999/XSL/Transform", (String) sources[2]);
					}

					String path = sources[1].toString();

					final XPathExpression expression = xpath.compile(path);

					return expression.evaluate(sources[0], returnType);

				} catch (XPathExpressionException ioex) {

					logException(caller, ioex, sources);
					return null;
				}
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_XPATH;
	}

	@Override
	public String shortDescription() {
		return "Returns the value of the given XPath expression from the given XML DOM. The optional third parameter defines the return type, possible values are: NUMBER, STRING, BOOLEAN, NODESET, NODE, default is STRING.";
	}
}
