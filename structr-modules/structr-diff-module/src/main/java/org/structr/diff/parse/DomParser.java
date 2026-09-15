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
package org.structr.diff.parse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.structr.diff.model.Entity;
import org.structr.diff.model.Kind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

/**
 * A page, component or template file, decomposed into elements and text.
 *
 * Parsed with jsoup's XML parser rather than its HTML one, the way Importer does it: the export
 * contains {@code structr:template} and {@code structr:component} tags, and the HTML parser would
 * restructure the document around them.
 *
 * <p>Elements are keyed by {@code data-structr-meta-id}, which measurement says is worth relying
 * on: 2347 of 2348 element ids survived between two instances of the same app, the single loss
 * being an element that really was deleted. An element without one is keyed positionally, which is
 * weaker but only affects elements the exporter did not identify.
 *
 * <p>Text has no id of its own. In the export it is written inline inside its parent, so it is
 * keyed by parent plus ordinal: a stable anchor, since the parent is uuid-keyed, but two sibling
 * text nodes under one parent can only be told apart by position, and swapping them reads as two
 * edits rather than a move. That is a limit of the format, not of the model.
 *
 * <p>The {@code @structr:owner} and {@code @structr:grant} comments preceding an element are its
 * metadata rather than page content, so they are folded into the element they describe. Any other
 * comment is content and stays where it is.
 */
public class DomParser {

	/**
	 * Tags whose content is text, not markup.
	 *
	 * An XML parser has no notion of this, so a {@code <script>} holding {@code a < b} or a snippet
	 * of HTML gets parsed into elements, and the body is split into fragments around them. One real
	 * app had 26 such sequences in a single stored template, producing 26 phantom elements and 31
	 * phantom text nodes from that file alone.
	 *
	 * <p>{@code Parser.xmlParser().tagSet(TagSet.Html())} looks like the answer and is not: the
	 * option is ignored by the XML tree builder, and the counts do not move. Handling it in the walk
	 * is what actually works.
	 */
	private static final Set<String> DATA_ELEMENTS = Set.of("script", "style");

	/**
	 * HTML elements that cannot have children, and that an export writes unclosed.
	 *
	 * To an XML parser an unclosed {@code <img>} is an open container that swallows its following
	 * siblings, which corrupts the parent and ordinal of everything after it. One app had 126.
	 */
	private static final Set<String> VOID_ELEMENTS = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr");

	private static final String META_ID   = "data-structr-meta-id";
	private static final String META_NAME = "data-structr-meta-name";

	private final List<Entity> entities;
	private final String origin;

	public DomParser(final String markup, final String origin, final List<Entity> target) {

		this.entities = target;
		this.origin   = origin;

		final Document document = Jsoup.parse(markup, "", Parser.xmlParser());

		unwrapVoidElements(document);
		walk(document, origin);
	}

	/** Lifts the children of a void element up to its parent, keeping their order. */
	private static void unwrapVoidElements(final Element root) {

		for (final Element element : root.getAllElements()) {

			if (!VOID_ELEMENTS.contains(element.tagName().toLowerCase()) || element.childNodeSize() == 0) {

				continue;
			}

			Node anchor = element;

			for (final Node child : new ArrayList<>(element.childNodes())) {

				anchor.after(child);
				anchor = child;
			}
		}
	}

	 /**
	 * Children of one node, in order, assigning each an ordinal within that parent.
	 *
	 * The ordinal counts elements and text together rather than each separately, so that
	 * inserting an element before a text node is visible as the text node moving.
	 */
	private void walk(final Node parent, final String parentKey) {

		int ordinal            = 0;
		String pendingMetadata = null;

		for (final Node child : parent.childNodes()) {

			if (child instanceof Comment comment) {

				final String data = comment.getData().strip();
				if (data.startsWith("@structr:")) {

					pendingMetadata = data;

				} else {

					addContent(parentKey, ordinal++, comment.toString());
				}

				continue;
			}

			if (child instanceof TextNode text) {

				// whitespace between tags lays the file out and belongs to no entity. Text that
				// is only whitespace inside a parent that has other children is that, and
				// dropping it here is what keeps indentation from ever reaching the matcher.
				if (!text.getWholeText().isBlank()) {

					addContent(parentKey, ordinal++, text.getWholeText());
				}

				continue;
			}

			if (child instanceof Element element) {

				final String key = addElement(element, parentKey, ordinal++, pendingMetadata);

				pendingMetadata = null;

				if (DATA_ELEMENTS.contains(element.tagName().toLowerCase())) {

					// its content is text: take it whole rather than walking what the XML parser made of it
					final String data = element.html();
					if (!data.isBlank()) {

						addContent(key, 0, data);
					}

				} else {

					walk(element, key);
				}
			}
		}
	}

	private void addContent(final String parentKey, final int ordinal, final String text) {

		entities.add(new Entity(Kind.CONTENT, parentKey + "#" + ordinal, null, parentKey, null, null, text, ordinal, origin));
	}

	private String addElement(final Element element, final String parentKey, final int ordinal, final String metadata) {

		final Map<String, Object> attributes = new TreeMap<>();
		final String metaId                  = element.attr(META_ID);
		final String key                     = !metaId.isEmpty() ? metaId : parentKey + "/" + element.tagName() + "#" + ordinal;

		attributes.put("tag", element.tagName());

		for (final org.jsoup.nodes.Attribute attribute : element.attributes()) {

			if (!META_ID.equals(attribute.getKey())) {

				attributes.put(attribute.getKey(), attribute.getValue());
			}
		}

		if (metadata != null) {

			attributes.put("structr:metadata", metadata);
		}

		final String name = !element.attr(META_NAME).isEmpty() ? element.attr(META_NAME) : element.tagName();

		entities.add(new Entity(Kind.DOM_ELEMENT, key, null, parentKey, name, attributes, null, ordinal, origin));

		return key;
	}
}
