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
package org.structr.rest.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.Reader;
import java.util.*;

/**
 *
 */
public class XMLStructureAnalyzer {

	private static final Logger logger          = LoggerFactory.getLogger(XMLStructureAnalyzer.class);
	private static final int XML_MAX_EXCEPTIONS = 100;

	private final Map<String, Object> structure = new LinkedHashMap<>();
	private XMLInputFactory factory             = null;
	private XMLEventReader reader               = null;
	private Element current                     = null;
	private int analysisCount                   = 0;

	public XMLStructureAnalyzer(final Reader input) throws XMLStreamException {

		this.factory = XMLInputFactory.newInstance();

		// disable DTD referencing, namespaces etc
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);

		// create XML reader
		this.reader = factory.createXMLEventReader(input);
	}

	public Map<String, Object> getStructure(final int threshold) {

		int level = 0;
		int exceptionCount = 0;

		while (reader.hasNext() && analysisCount < threshold && exceptionCount < XML_MAX_EXCEPTIONS) {

			try {
				final XMLEvent event = reader.nextEvent();
				final String tagName = getTagName(event);

				switch (event.getEventType()) {

					case XMLEvent.START_ELEMENT:
						current = new Element(current, tagName, level++);
						current.attributes.addAll(getAttributes(event.asStartElement()));
						break;

					case XMLEvent.END_ELEMENT:

						// one level up
						current = current.parent;

						level--;

						// start analysis for elements with more than one occurrence
						if (current != null && current.level == 0) {

							analyze(current, structure);
							analysisCount++;
						}
						break;
				}

			} catch (XMLStreamException strex) {
				logger.warn(strex.getMessage());
				exceptionCount++;
			}
		}

		if (exceptionCount == XML_MAX_EXCEPTIONS) {
			logger.info("Stopping XML processing at error threshold ({})", XML_MAX_EXCEPTIONS);
		}

		return structure;
	}

	// ----- private methods -----
	private String getTagName(final XMLEvent event) {

		if (event.isStartElement()) {
			return event.asStartElement().getName().toString();
		}

		if (event.isEndElement()) {
			return event.asEndElement().getName().toString();
		}

		return null;
	}


	private void analyze(final Element parent, final Map<String, Object> map) {

		Map<String, Object> currentObject = (Map)map.get(parent.tagName);
		if (currentObject == null) {

			currentObject = new LinkedHashMap<>();
			map.put(parent.tagName, currentObject);
		}

		// store attributes (if present)
		if (!parent.attributes.isEmpty()) {

			Set<String> attributes = (Set<String>)currentObject.get("::attributes");
			if (attributes == null) {

				attributes = new LinkedHashSet<>();
				currentObject.put("::attributes", attributes);
			}

			// store attributes
			attributes.addAll(parent.attributes);
		}

		// recurse
		for (Element child : parent.children) {
			analyze(child, currentObject);
		}
	}

	private Set<String> getAttributes(final StartElement startElement) {

		final Set<String> attributes = new LinkedHashSet<>();
		final Iterator iterator      = startElement.getAttributes();

		while (iterator.hasNext()) {

			final Attribute attr = (Attribute)iterator.next();
			attributes.add(attr.getName().toString());
		}

		return attributes;
	}

	// ----- nested classes -----
	private class Element {

		private final List<Element> children = new LinkedList<>();
		private final Set<String> attributes = new LinkedHashSet<>();
		private Element parent               = null;
		private String tagName               = null;
		private int level                    = 0;

		public Element(final Element parent, final String tagName, final int level) {

			this.parent  = parent;
			this.tagName = tagName;
			this.level   = level;

			if (parent != null) {
				parent.children.add(this);
			}
		}
	}
}
