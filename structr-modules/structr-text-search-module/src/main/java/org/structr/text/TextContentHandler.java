/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.*;
import java.util.regex.Pattern;

/**
 *
 */
public class TextContentHandler implements ContentHandler {

	private static final Logger logger = LoggerFactory.getLogger(TextContentHandler.class);

	private final Map<String, String> meta  = new LinkedHashMap<>();
	private final Deque<String> path        = new LinkedList<>();
	private final StringBuilder lineBuffer  = new StringBuilder();
	private final List<AnnotatedPage> pages = new ArrayList<>();
	private final Context context           = new Context();
	private AnnotatedPage page              = null;

	@Override
	public void setDocumentLocator(final Locator locator) {
	}

	@Override
	public void startDocument() throws SAXException {
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

		path.add(localName);

		switch (getPath()) {

			case "/html/body/div":

				if ("page".equals(atts.getValue("class"))) {
					nextPage();
				}
				break;

			case "/html/head/meta":

				String name  = null;
				String value = null;

				for (int i=0; i<atts.getLength(); i++) {

					final String metaKey = atts.getLocalName(i);

					switch (metaKey) {

						case "name":
							name = atts.getValue(i);
							break;

						case "content":
							value = atts.getValue(i);
							break;

						default:
							logger.warn("Unknown meta key {}", metaKey);
					}
				}

				if (name != null && value != null) {

					meta.put(name, value);
				}
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		nextLine();

		path.removeLast();
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {

		int newline = 0;

		for (int i=0; i<length; i++) {

			switch (ch[i]) {

				case '\n':
					if (newline == 0) {
						lineBuffer.append(" ");
					}
					newline++;
					break;

				default:
					lineBuffer.append(ch[i]);
					if (newline > 1) {
						nextLine();
						newline = 0;
					}
					break;
			}
		}
	}

	@Override
	public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
		characters(ch, start, length);
	}

	@Override
	public void processingInstruction(final String target, final String data) throws SAXException {
	}

	@Override
	public void skippedEntity(final String name) throws SAXException {
	}

	public List<AnnotatedPage> getPages() {
		return pages;
	}

	public void analyze() {

		final Pattern chapterPattern1 = Pattern.compile("[0-9\\.]* *[\\wöäüÖÄÜß -]+");
		final Pattern chapterPattern2 = Pattern.compile("[\\wöäüÖÄÜß -]+ *[0-9\\.]*");
		final Pattern wordPattern     = Pattern.compile("[\\wöäüÖÄÜß-]+");

		for (final AnnotatedPage page : pages) {
			// iterate lines and try to obtain structural information
			for (final AnnotatedLine line : page.getLines()) {

				double headingProbability = 0.0;

				final String content = line.getContent();

				// line is probably a title if it is a single word
				if (wordPattern.matcher(content).matches()) {

					headingProbability += 0.2;
				}

				// line is probably a title if does not end with a full stop
				if (!content.endsWith(".")) {

					headingProbability += 0.2;
				}

				// line is probably a title if matches a "chapter heading" pattern
				if (chapterPattern1.matcher(content).matches()) {

					headingProbability += 0.2;
				}

				// line is probably a title if matches a "chapter heading" pattern
				if (chapterPattern2.matcher(content).matches()) {

					headingProbability += 0.2;
				}

				final String[] words   = content.split("[ ]+");
				int uppercaseWordCount = 0;
				int wordCount          = 0;

				// most words start with an uppercase letter
				for (final String word : words) {

					if (Character.isUpperCase(word.charAt(0))) {

						uppercaseWordCount++;
					}

					if (wordPattern.matcher(word).matches()) {
						wordCount++;
					}
				}

				if (uppercaseWordCount >= wordCount / 1.5) {
					headingProbability += 0.2;
				}

				// very simple heuristic approach
				if (headingProbability > 0.5) {
					line.setType("heading");
				}
			}
		}
	}

	public Map<String, String> getMetadata() {
		return meta;
	}


	// ----- private methods -----
	private void nextPage() {

		page = new AnnotatedPage();
		pages.add(page);
	}

	private void nextLine() {

		if (lineBuffer.length() > 0) {

			final String line                 = lineBuffer.toString();
			final AnnotatedLine annotatedLine = new AnnotatedLine(line);

			if (annotatedLine.includeLine(context)) {

				annotatedLine.transformAndAnalyze();

				if (page != null) {

					page.addLine(annotatedLine);

				} else {

					logger.warn("No page for content {}", line);
				}
			}

			lineBuffer.setLength(0);
		}
	}

	private String getPath() {
		return "/" + StringUtils.join(path, "/");
	}
}
