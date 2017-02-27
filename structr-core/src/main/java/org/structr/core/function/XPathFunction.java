/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.w3c.dom.Document;

/**
 *
 */
public class XPathFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_XPATH = "Usage: ${xpath(xmlDocument, expression)}. Example: ${xpath(xml(this.xmlSource), \"/test/testValue\")}";

	@Override
	public String getName() {
		return "xpath()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (!(arrayHasLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof Document)) {
				
				return null;
			}

			try {

				XPath xpath = XPathFactory.newInstance().newXPath();
				return xpath.evaluate(sources[1].toString(), sources[0], XPathConstants.STRING);

			} catch (XPathExpressionException ioex) {

				logException(caller, ioex, sources);
				return null;

			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_XPATH;
	}

	@Override
	public String shortDescription() {
		return "Returns the value of the given XPath expression from the given XML DOM";
	}

}
