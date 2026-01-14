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

import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class XmlFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "xml";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("xmlString");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				try {

					final DocumentBuilder builder = getDocumentBuilder();

					if (builder != null) {

						final String xml = (String)sources[0];
						final StringReader reader = new StringReader(xml);
						final InputSource src = new InputSource(reader);

						return builder.parse(src);
					}

				} catch (IOException | SAXException | ParserConfigurationException ex) {

					logException(caller, ex, sources);
				}

				return "";
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.xml(xmlString) }}. Example: ${{ $.xpath($.xml($.this.text), \"/test/testValue\") }}"),
			Usage.structrScript("Usage: ${xml(xmlString)}. Example: ${xpath(xml(this.text), \"/test/testValue\")}")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${xml(read('test.xml'))}", "Read file test.xml from exchange/ directory and return parsed document"),
				Example.structrScript("${xml(getContent(first(find('File', 'name', 'test.xml'))))}", "Read first file named test.xml from virtual filesystem and return parsed document")
		);
	}

	@Override
	public String getShortDescription() {
		return "Tries to parse the contents of the given string into an XML document, returning the document on success.";
	}

	@Override
	public String getLongDescription() {
		return """
		This function can be used in conjunction with `xpath()` to extract data from an XML document.

		By default, the following features of DocumentBuilderFactory are active to protect against malicious input.
		This is controlled via the configuration setting `%s`:

		```
		factory.setNamespaceAware(true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		```
		""".formatted(Settings.XMLParserSecurity.getKey());
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Disabling `%s` reduces protection against malicious input. Do this only when the data source is fully trusted and you accept the associated risks.".formatted(Settings.XMLParserSecurity.getKey())
		);
	}

	public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		if (Settings.XMLParserSecurity.getValue()) {

			factory.setNamespaceAware(true);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		}

		return factory.newDocumentBuilder();
	}
}
