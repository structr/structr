/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.parser;

import org.structr.extraction.api.document.DocumentBlockType;
import org.structr.extraction.api.document.ExtractedLink;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Collects the structure of a document from the XHTML events Tika emits: headings with the section path
 * they open, paragraphs, list items with their nesting depth, tables, code blocks, quotes and links, and
 * the page (or slide) each of them is on.
 *
 * Tika's XHTML is uniform across formats, but not every format uses every element: plain text and OCR
 * output arrive as one paragraph or as loose text, so text is additionally split into paragraphs at
 * blank lines.
 *
 * Embedded documents (archive entries, attachments, embedded objects and images) arrive wrapped in a
 * {@code <div class="embedded-document" data-name="...">}, see {@code TikaParserSetup}. Their names make
 * up the embedded path of the blocks inside, and headings and pages start over in every embedded document.
 */
public class XhtmlStructureHandler extends DefaultHandler {

	/** Class of the element that wraps an embedded document. */
	public static final String EMBEDDED_DOCUMENT_CLASS = "embedded-document";

	/** Attribute of that element with the name of the embedded document, if it has one. */
	public static final String EMBEDDED_NAME_ATTRIBUTE = "data-name";

	private static final Pattern BLANK_LINES = Pattern.compile("\\n\\s*\\n");
	private static final Pattern WHITESPACE  = Pattern.compile("\\s+");

	private final List<ParsedBlock> blocks           = new ArrayList<>();
	private final List<ExtractedLink> links       = new ArrayList<>();
	private final LinkedList<Heading> headings    = new LinkedList<>();
	private final StringBuilder buffer            = new StringBuilder();
	private final int maxCharacters;

	private DocumentBlockType currentType     = null;
	private int currentLevel          = 0;
	private int listDepth             = 0;
	private int quoteDepth            = 0;
	private int skipDepth             = 0;
	private int pageCount             = 0;
	private int collectedCharacters   = 0;
	private boolean truncated         = false;

	// tables: cells collect their own text, a nested table is flattened into the cell of the outer one
	private List<List<String>> tableRows = null;
	private List<String> tableRow        = null;
	private StringBuilder cellBuffer     = null;
	private int tableDepth               = 0;

	// links: the anchor text is collected while the <a> element is open
	private String linkHref               = null;
	private StringBuilder linkText        = null;

	// embedded documents: the ones the parser is in, innermost last, and for each open div whether it
	// opened one
	private final LinkedList<Entry> entries = new LinkedList<>();
	private final LinkedList<Boolean> divs  = new LinkedList<>();
	private int pageCharacters              = 0;

	public XhtmlStructureHandler(final int maxCharacters) {

		this.maxCharacters = maxCharacters;
	}

	public List<ParsedBlock> getBlocks() {

		return blocks;
	}

	public List<ExtractedLink> getLinks() {

		return links;
	}

	/**
	 * Number of pages (or slides) the document announced through page elements, 0 if it has none.
	 */
	public int getPageCount() {

		return pageCount;
	}

	public boolean isTruncated() {

		return truncated;
	}

	/**
	 * Number of non-whitespace characters on the current page (or slide) of the innermost document so far,
	 * not counting the content of documents embedded in it.
	 */
	public int getCurrentPageCharacters() {

		return pageCharacters;
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {

		final String name = elementName(localName, qName);
		if (skipDepth > 0 || "head".equals(name) || "script".equals(name) || "style".equals(name)) {

			skipDepth++;

			return;
		}

		if ("a".equals(name)) {

			linkHref = attributes.getValue("href");
			linkText = new StringBuilder();

			return;
		}

		if ("br".equals(name)) {

			append("\n");

			return;
		}

		// inside a table cell, block elements only separate words
		if (cellBuffer != null) {

			if ("table".equals(name)) {

				tableDepth++;
			}

			cellBuffer.append(' ');

			return;
		}

		switch (name) {

			case "div" -> {

				flush();

				final String cssClass = attributes.getValue("class");
				final boolean entry   = EMBEDDED_DOCUMENT_CLASS.equals(cssClass);

				divs.addLast(entry);

				if (entry) {

					startEntry(attributes.getValue(EMBEDDED_NAME_ATTRIBUTE));

				} else if (cssClass != null && (cssClass.equals("page") || cssClass.equals("slide-content"))) {

					pageCount++;
					pageCharacters = 0;
				}
			}

			case "h1", "h2", "h3", "h4", "h5", "h6" -> start(DocumentBlockType.HEADING, name.charAt(1) - '0');

			case "p" -> {

				// a paragraph directly inside a list item or quote continues it instead of replacing it
				if (currentType == DocumentBlockType.LIST_ITEM && buffer.isEmpty()) {

					return;
				}

				start(DocumentBlockType.PARAGRAPH, 0);
			}

			case "li" -> start(DocumentBlockType.LIST_ITEM, Math.max(1, listDepth));

			case "ul", "ol" -> {

				flush();
				listDepth++;
			}

			case "pre" -> start(DocumentBlockType.CODE, 0);

			case "blockquote" -> {

				flush();
				quoteDepth++;
			}

			case "table" -> {

				flush();
				tableRows  = new ArrayList<>();
				tableDepth = 1;
			}

			case "tr" -> tableRow = new ArrayList<>();

			case "td", "th" -> cellBuffer = new StringBuilder();

			default -> {
			}
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) {

		final String name = elementName(localName, qName);

		if (skipDepth > 0) {

			skipDepth--;

			return;
		}

		if ("a".equals(name)) {

			if (linkHref != null && !linkHref.isBlank()) {

				links.add(new ExtractedLink(linkHref, normalize(linkText.toString()), currentPage()));
			}

			linkHref = null;
			linkText = null;

			return;
		}

		if (cellBuffer != null) {

			if ("table".equals(name) && tableDepth > 1) {

				tableDepth--;
				cellBuffer.append(' ');

				return;
			}

			if ("td".equals(name) || "th".equals(name)) {

				if (tableRow != null) {

					tableRow.add(normalize(cellBuffer.toString()));
				}

				cellBuffer = null;

				return;
			}

			cellBuffer.append(' ');

			return;
		}

		switch (name) {

			case "h1", "h2", "h3", "h4", "h5", "h6", "p", "li", "pre" -> flush();

			case "div" -> {

				flush();

				if (!divs.isEmpty() && divs.removeLast()) {

					endEntry();
				}
			}

			case "ul", "ol" -> {

				flush();
				listDepth = Math.max(0, listDepth - 1);
			}

			case "blockquote" -> {

				flush();
				quoteDepth = Math.max(0, quoteDepth - 1);
			}

			case "tr" -> {

				if (tableRows != null && tableRow != null && tableRow.stream().anyMatch(cell -> !cell.isEmpty())) {

					tableRows.add(tableRow);
				}

				tableRow = null;
			}

			case "table" -> {

				if (tableRows != null && !tableRows.isEmpty()) {

					blocks.add(new ParsedBlock(DocumentBlockType.TABLE, 0, null, currentPage(), headingPath(), tableRows, embeddedPath()));
				}

				tableRows  = null;
				tableDepth = 0;
			}

			case "body" -> flush();

			default -> {
			}
		}
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) {

		if (skipDepth > 0) {

			return;
		}

		if (truncated) {

			return;
		}

		final int remaining = maxCharacters - collectedCharacters;
		final int accepted  = Math.min(length, remaining);

		if (accepted < length) {

			truncated = true;
		}

		if (accepted <= 0) {

			return;
		}

		collectedCharacters += accepted;

		final String text = new String(ch, start, accepted);

		pageCharacters += (int) text.chars().filter(c -> !Character.isWhitespace(c)).count();

		if (linkText != null) {

			linkText.append(text);
		}

		append(text);
	}

	@Override
	public void ignorableWhitespace(final char[] ch, final int start, final int length) {

		if (skipDepth == 0) {

			append(new String(ch, start, length));
		}
	}

	@Override
	public void endDocument() {

		flush();
	}

	// ----- private methods -----
	private void append(final String text) {

		if (cellBuffer != null) {

			cellBuffer.append(text);

		} else {

			buffer.append(text);
		}
	}

	private void start(final DocumentBlockType type, final int level) {

		flush();

		currentType  = type;
		currentLevel = level;
	}

	/**
	 * Turns the collected text into blocks of the current type and resets the buffer. Text that arrived
	 * outside of any block element becomes paragraphs.
	 */
	private void flush() {

		final DocumentBlockType type = currentType != null ? currentType : DocumentBlockType.PARAGRAPH;
		final int level      = currentLevel;
		final String text    = buffer.toString();

		buffer.setLength(0);
		currentType  = null;
		currentLevel = 0;

		if (text.isBlank()) {

			return;
		}

		if (type == DocumentBlockType.CODE) {

			blocks.add(new ParsedBlock(DocumentBlockType.CODE, 0, text.strip(), currentPage(), headingPath(), List.of(), embeddedPath()));

			return;
		}

		if (type == DocumentBlockType.HEADING) {

			final String title = normalize(text);

			while (!headings.isEmpty() && headings.getLast().level() >= level) {

				headings.removeLast();
			}

			headings.addLast(new Heading(level, title));

			blocks.add(new ParsedBlock(DocumentBlockType.HEADING, level, title, currentPage(), headingPath(), List.of(), embeddedPath()));

			return;
		}

		final DocumentBlockType effectiveType = (type == DocumentBlockType.PARAGRAPH && quoteDepth > 0) ? DocumentBlockType.QUOTE : type;

		for (final String part : BLANK_LINES.split(text)) {

			final String normalized = normalize(part);
			if (!normalized.isEmpty()) {

				blocks.add(new ParsedBlock(effectiveType, level, normalized, currentPage(), headingPath(), List.of(), embeddedPath()));
			}
		}
	}

	/**
	 * The page of the innermost document, or for an embedded document without pages (e.g. an image), the
	 * page of the document it is embedded in.
	 */
	private Integer currentPage() {

		if (pageCount > 0) {

			return pageCount;
		}

		return entries.isEmpty() ? null : entries.getLast().page;
	}

	private List<String> headingPath() {

		return headings.stream().map(Heading::title).toList();
	}

	/**
	 * The names of the embedded documents the parser is in, outermost first, joined with "/"; null outside
	 * of embedded documents. Embedded documents without a name (e.g. the body of an e-mail) are left out.
	 */
	private String embeddedPath() {

		final List<String> names = entries.stream().map(Entry::name).filter(Objects::nonNull).toList();

		return names.isEmpty() ? null : String.join("/", names);
	}

	private void startEntry(final String name) {

		entries.addLast(new Entry(name != null && !name.isBlank() ? name.strip() : null, new ArrayList<>(headings), pageCount, pageCharacters, currentPage()));

		headings.clear();

		pageCount      = 0;
		pageCharacters = 0;
	}

	private void endEntry() {

		final Entry entry = entries.removeLast();

		headings.clear();
		headings.addAll(entry.headings);

		pageCount      = entry.pageCount;
		pageCharacters = entry.pageCharacters;
	}

	private static String normalize(final String text) {

		return WHITESPACE.matcher(text).replaceAll(" ").strip();
	}

	private static String elementName(final String localName, final String qName) {

		final String name = (localName != null && !localName.isEmpty()) ? localName : qName;

		return name != null ? name.toLowerCase(Locale.ROOT) : "";
	}

	private record Heading(int level, String title) {
	}

	/**
	 * An embedded document the parser is in: its name (null if it has none), and the state of the
	 * enclosing document to restore when it ends.
	 */
	private record Entry(String name, List<Heading> headings, int pageCount, int pageCharacters, Integer page) {
	}
}
