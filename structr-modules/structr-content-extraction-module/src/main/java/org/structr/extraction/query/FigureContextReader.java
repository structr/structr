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
package org.structr.extraction.query;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.document.FigureContext;
import org.structr.extraction.api.search.ContentSearchHit;
import org.structr.extraction.api.search.ContentSearchMode;
import org.structr.extraction.api.search.ContentSearchOptions;
import org.structr.extraction.chunking.token.TokenCounter;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.text.StopWords;
import org.structr.web.entity.File;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Collects what a document says about one of its figures: the caption and the sentences that refer to
 * it, the text of its page, and other passages about it found by the words of its caption. Reads only.
 */
public final class FigureContextReader {

	/** Words of a caption that are searched for at most; more make the search slower, not better. */
	private static final int MAX_TERMS = 12;

	private FigureContextReader() {
	}

	/**
	 * @param document  the document the figure is from
	 * @param label     the figure's label; may be null
	 * @param caption   the figure's caption; may be null
	 * @param page      the figure's page; may be null
	 * @param mentions  the sentences that refer to the figure
	 * @param maxTokens the token budget of the assembled text
	 * @param passages  how many other passages to find; 0 for none
	 */
	public static FigureContext read(final SecurityContext securityContext, final File document, final String label, final String caption, final Integer page, final List<String> mentions,
		final int maxTokens, final int passages) throws FrameworkException {

		final List<ExtractedContentNode> chunks = ContentChunkReader.chunks(document);
		final List<ExtractedContentNode> onPage = new ArrayList<>();
		final Set<String> onPageIds             = new HashSet<>();

		if (page != null) {

			for (final ExtractedContentNode chunk : chunks) {

				if (chunk.getPageStart() != null && chunk.getPageEnd() != null && chunk.getPageStart() <= page && chunk.getPageEnd() >= page) {

					onPage.add(chunk);
					onPageIds.add(chunk.getUuid());
				}
			}
		}

		final String pageText                      = ContentChunkReader.mergedText(onPage);
		final List<FigureContext.Passage> related  = chunks.isEmpty() || passages <= 0 ? List.of() : related(securityContext, document, caption, onPageIds, passages);
		final TokenCounter counter                 = TokenCounter.forEncoding(ContentExtractionSettings.TokenEncoding.getValue());
		final String text                          = assemble(counter, document, label, caption, page, mentions, pageText, related, maxTokens);

		return new FigureContext(label, caption, page, mentions, pageText, related, text, counter.count(text), !chunks.isEmpty());
	}

	// ----- private methods -----
	/**
	 * Passages with words of the caption, from other places than the figure's page.
	 */
	private static List<FigureContext.Passage> related(final SecurityContext securityContext, final File document, final String caption, final Set<String> onPage, final int limit) throws FrameworkException {

		final String query = query(caption);
		if (query == null) {

			return List.of();
		}

		final List<FigureContext.Passage> result = new ArrayList<>();
		final ContentSearchOptions options       = new ContentSearchOptions(limit + onPage.size(), 0.0, Set.of(document.getUuid()), null, 1, ContentSearchMode.LUCENE);

		for (final ContentSearchHit hit : ContentExtraction.search(securityContext, query, options)) {

			// the page is in the context already, and the text of figures (e.g. an earlier interpretation) is no source
			if (!onPage.contains(hit.node().getUuid()) && hit.node().getFigure() == null) {

				result.add(new FigureContext.Passage(hit.context() != null ? hit.context() : hit.node().getText(), hit.node().getPageStart()));

				if (result.size() >= limit) {

					break;
				}
			}
		}

		return result;
	}

	/**
	 * Any of the significant words of the caption, as a Lucene query; null if it has none.
	 */
	private static String query(final String caption) {

		if (caption == null || caption.isBlank()) {

			return null;
		}

		final Set<String> stopWords = StopWords.forLanguage(ContentExtraction.detectLanguage(caption).language());
		final Set<String> terms     = new LinkedHashSet<>();

		// only letters and digits, so nothing in a term means something to Lucene
		for (final String word : caption.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {

			if (word.length() >= 4 && !stopWords.contains(word) && terms.size() < MAX_TERMS) {

				terms.add(word);
			}
		}

		return terms.isEmpty() ? null : String.join(" OR ", terms);
	}

	/**
	 * The parts in order of importance; the first one that does not fit is cut, the rest is left out.
	 */
	private static String assemble(final TokenCounter counter, final File document, final String label, final String caption, final Integer page, final List<String> mentions,
		final String pageText, final List<FigureContext.Passage> related, final int maxTokens) {

		final List<String> parts = new ArrayList<>();
		final String title       = label != null && caption != null && caption.startsWith(label) ? caption : join(label, caption);

		parts.add("Document: " + document.getName() + (title.isEmpty() ? "" : "\n" + "Figure" + (page != null ? " on page " + page : "") + ": " + title));

		if (!mentions.isEmpty()) {

			parts.add("Sentences of the document that refer to the figure:\n" + String.join("\n", mentions.stream().map(mention -> "- " + mention).toList()));
		}

		if (!pageText.isBlank()) {

			parts.add("Text of page " + page + ":\n" + pageText);
		}

		if (!related.isEmpty()) {

			parts.add("Other passages of the document about the figure:\n" + String.join("\n\n", related.stream().map(passage -> (passage.page() != null ? "[page " + passage.page() + "] " : "") + passage.text()).toList()));
		}

		final StringBuilder text = new StringBuilder();

		for (final String part : parts) {

			final String next = text.isEmpty() ? part : text + "\n\n" + part;

			if (counter.count(next) <= maxTokens) {

				text.setLength(0);
				text.append(next);

			} else {

				final String cut = counter.truncate(next, maxTokens);

				text.setLength(0);
				text.append(cut);

				break;
			}
		}

		return text.toString();
	}

	private static String join(final String label, final String caption) {

		if (label == null) {

			return caption != null ? caption : "";
		}

		return caption != null ? label + ": " + caption : label;
	}
}
