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
package org.structr.extraction.api.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a document says about one of its figures, for a prompt that interprets the figure in its context.
 *
 * @param label     the figure's label, e.g. "Figure 3"; may be null
 * @param caption   the figure's caption; may be null
 * @param page      the page the figure is on; null for a document without pages
 * @param mentions  the sentences of the document that refer to the figure
 * @param pageText  the extracted text of the figure's page
 * @param related   other passages of the document about the figure, best first
 * @param text      all of it as one text with headings, within the token budget
 * @param tokens    the number of tokens of {@code text}
 * @param extracted whether the document has extracted content; without it, page text and passages are empty
 */
public record FigureContext(String label, String caption, Integer page, List<String> mentions, String pageText, List<Passage> related, String text, int tokens, boolean extracted) {

	public FigureContext {

		mentions = List.copyOf(mentions);
		related  = List.copyOf(related);
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("label",      label);
		map.put("caption",    caption);
		map.put("page",       page);
		map.put("mentions",   mentions);
		map.put("pageText",   pageText);
		map.put("related",    related.stream().map(Passage::toMap).toList());
		map.put("text",       text);
		map.put("tokenCount", tokens);
		map.put("extracted",  extracted);

		return map;
	}

	/**
	 * A passage of the document.
	 *
	 * @param text the passage, with the chunks around the match
	 * @param page the first page of the passage; null for a document without pages
	 */
	public record Passage(String text, Integer page) {

		public Map<String, Object> toMap() {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("text", text);
			map.put("page", page);

			return map;
		}
	}
}
