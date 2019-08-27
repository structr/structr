/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.text;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.ContentContainer;
import org.structr.web.entity.ContentItem;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 *
 */
public class DocumentContentHandler implements ContentHandler {

	private static final Logger logger = LoggerFactory.getLogger(DocumentContentHandler.class);

	private Map<String, Integer> pathCountMap  = new LinkedHashMap<>();
	private ContentContainer currentContainer  = null;
	private ContentItem currentItem            = null;
	private String name                        = null;
	private App app                            = null;

	public DocumentContentHandler(final SecurityContext securityContext, final String name) {

		this.app  = StructrApp.getInstance(securityContext);
		this.name = name;
	}

	@Override
	public void setDocumentLocator(final Locator locator) {
	}

	@Override
	public void startDocument() throws SAXException {

		try {

			currentContainer = app.create(ContentContainer.class, name);

		} catch (FrameworkException ex) {
			logger.warn("Unable to create ContentContainer for document {}: {}", name, ex.getMessage());
		}

	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
	}

	@Override
	public void endPrefixMapping(final String prefix) throws SAXException {
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {

		try {

			switch (localName) {

				case "ul":
				case "div":
					currentContainer = app.create(ContentContainer.class,
						new NodeAttribute<>(AbstractNode.name, getContainerName(currentContainer, localName)),
						new NodeAttribute<>(StructrApp.key(ContentContainer.class, "parent"), currentContainer)
					);
					break;

				case "li":
				case "p":
				case "a":
					currentItem = app.create(ContentItem.class,
						new NodeAttribute<>(AbstractNode.name, getContainerName(currentContainer, localName)),
						new NodeAttribute<>(StructrApp.key(ContentItem.class, "containers"), Arrays.asList(currentContainer))
					);
					break;

				default:
					System.out.println("Ignoring " + localName + ", no case defined.");
			}

		} catch (FrameworkException ex) {
			logger.warn("Unable to create element for {}: {}", localName, ex.getMessage());
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {

		if (currentContainer != null) {

			switch (localName) {

				case "ul":
				case "div":
					handleEndElement();
					currentContainer = currentContainer.getParent();
					break;
			}
		}
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {

		try {

			final PropertyKey key = StructrApp.key(ContentItem.class, "content");
			final String content  = String.valueOf(ch, start, length);

			if (currentItem != null) {

				final StringBuilder buf = new StringBuilder();

				if (StringUtils.isNotBlank(content)) {


						final String existingContent = (String)currentItem.getProperty(key);
						if (existingContent != null) {

							buf.append(existingContent);
							buf.append(" ");
						}

						buf.append(content);

						currentItem.setProperty(key, buf.toString());
				}

			} else {

				// create anonymous content item for character content
				currentItem = app.create(ContentItem.class,
					new NodeAttribute<>(AbstractNode.name, getContainerName(currentContainer, "content")),
					new NodeAttribute<>(StructrApp.key(ContentItem.class, "containers"), Arrays.asList(currentContainer)),
					new NodeAttribute<>(key, content)
				);
			}

		} catch (FrameworkException ex) {
			logger.warn("Unable to store content: {}", ex.getMessage());
		}

	}

	@Override
	public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
	}

	@Override
	public void processingInstruction(final String target, final String data) throws SAXException {
	}

	@Override
	public void skippedEntity(final String name) throws SAXException {
	}

	// ----- private methods -----
	private void handleEndElement() {

		try {

			final PropertyKey key = StructrApp.key(ContentItem.class, "content");
			String title           = null;

			for (final ContentItem item : currentContainer.getItems()) {

				// remove empty content items
				final String content = (String)item.getProperty(key);
				if (StringUtils.isBlank(content)) {

					app.delete(item);

				} else if (title == null) {

					title = (String)item.getProperty(key);
				}
			}

			if (StringUtils.isNotBlank(title)) {

				// set name to the first non-empty content element inside of this container
				currentContainer.setProperty(AbstractNode.name, StringUtils.abbreviate(title, 40));
			}

		} catch (FrameworkException ex) {
			logger.warn("Unable to handle end element: {}", ex.getMessage());
		}
	}

	private String getPath(final ContentContainer container) {

		if (container != null) {

			final List<String> ids   = new LinkedList<>();
			ContentContainer current = container;
			int count                = 0;

			while (current != null && count++ < 100) {

				// prepend UUID
				ids.add(0, current.getUuid());
				current = current.getParent();
			}

			return StringUtils.join(ids, "/");
		}

		return "/";
	}

	private Integer getAndIncrementCountForPath(final String path) {

		Integer count = pathCountMap.get(path);
		if (count == null) {

			pathCountMap.put(path, 1);

		} else {

			pathCountMap.put(path, count + 1);
		}

		return pathCountMap.get(path);
	}

	private String getContainerName(final ContentContainer container, final String tag) {
		return StringUtils.leftPad(getAndIncrementCountForPath(getPath(container)).toString(), 3, "0") + " " + tag;
	}
}
