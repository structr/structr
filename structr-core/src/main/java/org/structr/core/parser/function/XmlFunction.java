/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 */
public class XmlFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_XML = "Usage: ${xml(xmlSource)}. Example: ${xpath(xml(this.xmlSource), \"/test/testValue\")}";

	@Override
	public String getName() {
		return "xml()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof String) {

			try {

				final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				if (builder != null) {

					final String xml = (String)sources[0];
					final StringReader reader = new StringReader(xml);
					final InputSource src = new InputSource(reader);

					return builder.parse(src);
				}

			} catch (IOException | SAXException | ParserConfigurationException ex) {
				ex.printStackTrace();
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_XML;
	}

	@Override
	public String shortDescription() {
		return "Parses the given string to an XML DOM";
	}

}
