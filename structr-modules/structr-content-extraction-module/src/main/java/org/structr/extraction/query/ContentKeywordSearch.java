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
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.search.ContentSearchHit;
import org.structr.extraction.api.search.ContentSearchMode;
import org.structr.extraction.api.search.ContentSearchOptions;
import org.structr.extraction.api.search.FileSearchHit;
import org.structr.extraction.api.search.SearchSnippet;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.web.entity.File;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword search over the stored chunks of all files, through the fulltext index on their text.
 */
public final class ContentKeywordSearch {

	/** Search fetches this many times the requested hits, since filters and permissions drop some. */
	private static final int OVERFETCH = 10;

	/** Upper bound for the number of chunks fetched from the index for one search. */
	private static final int MAX_FETCH = 1000;

	/** Shorter words of an exact search are not asked from the index when there are longer ones. */
	private static final int MIN_EXACT_WORD_LENGTH = 3;

	private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

	private ContentKeywordSearch() {
	}

	/**
	 * Keyword search over chunk text, through the fulltext index. Hits are those the security context may
	 * read, so a chunk is found only if its file is readable.
	 */
	public static List<ContentSearchHit> search(final SecurityContext securityContext, final String query, final ContentSearchOptions options) throws FrameworkException {

		final List<ContentSearchHit> hits = new ArrayList<>();
		final String luceneQuery          = luceneQuery(query, options.mode());

		if (luceneQuery == null) {

			return hits;
		}

		for (final ChunkHit hit : chunkHits(securityContext, luceneQuery, options, options.limit() * OVERFETCH)) {

			hits.add(searchHit(hit, options));

			if (hits.size() >= options.limit()) {

				break;
			}
		}

		return hits;
	}

	/**
	 * Search for files: the input is read as simple search (every word, as a prefix or with typos,
	 * ignoring case), hits are grouped by file, and each file comes with the matching places in context.
	 * The words may be spread over several chunks of a file: the index is asked for chunks with any of
	 * them, and a file is kept if its matching chunks together contain all of them.
	 *
	 * @param options      limit (number of files), minScore, fileUuids and folder are used; mode and contextChunks are ignored
	 * @param maxSnippets  at most this many snippets per file
	 * @param contextChars at most this many characters of context on each side of a match
	 */
	public static List<FileSearchHit> searchFiles(final SecurityContext securityContext, final String input, final ContentSearchOptions options, final int maxSnippets, final int contextChars) throws FrameworkException {

		final ContentSearchQuery query = ContentSearchQuery.parse(input);
		if (query.isEmpty()) {

			return List.of();
		}

		// the best chunk of each file comes first, so the order of the map is the order of the files
		final Map<String, FileHits> files = new LinkedHashMap<>();

		for (final ChunkHit hit : chunkHits(securityContext, query.toLucene(false), options, MAX_FETCH)) {

			final ExtractedContentNode chunk = hit.chunk();
			final String text                = chunk.getText();
			final String embeddedPath        = chunk.getEmbeddedPath();
			final FileHits fileHits          = files.computeIfAbsent(hit.file().getUuid(), uuid -> new FileHits(hit.file(), hit.score(), new HashSet<>(), new ArrayList<>()));

			// the index covers the name of the embedded document (archive entry, attachment) as well
			fileHits.matchedTerms().addAll(query.matchedTerms(text));

			if (embeddedPath != null) {

				fileHits.matchedTerms().addAll(query.matchedTerms(embeddedPath));
			}

			final int missing = maxSnippets - fileHits.snippets().size();
			if (missing > 0) {

				final List<SearchSnippet> snippets = SearchSnippets.find(text, query, chunk.getPageStart(), contextChars, missing);

				// a chunk found only by the name of its embedded document shows that name
				if (snippets.isEmpty() && embeddedPath != null) {

					snippets.addAll(SearchSnippets.find(embeddedPath, query, chunk.getPageStart(), contextChars, 1));
				}

				fileHits.snippets().addAll(snippets);
			}
		}

		final int termCount = query.terms().size();

		return files.values().stream()
			.filter(f -> f.matchedTerms().size() == termCount)
			.limit(options.limit())
			.map(f -> new FileSearchHit(f.file(), f.score(), List.copyOf(f.snippets())))
			.toList();
	}

	/**
	 * Search for files that contain the input itself, like the global search matches the properties of
	 * nodes: no typos, no prefixes, case only ignored if asked for. Whitespace in the input matches any
	 * whitespace, since extracted text often breaks lines where the input has a space. The index is asked
	 * for chunks that contain all words of the input (inside longer words, too), and a chunk is kept if its
	 * text contains the input; a match across two chunks is not found.
	 *
	 * @param options      limit (number of files), minScore, fileUuids and folder are used; mode and contextChunks are ignored
	 * @param maxSnippets  at most this many snippets per file
	 * @param contextChars at most this many characters of context on each side of a match
	 */
	public static List<FileSearchHit> searchFilesExact(final SecurityContext securityContext, final String input, final boolean ignoreCase, final ContentSearchOptions options, final int maxSnippets, final int contextChars) throws FrameworkException {

		final Pattern pattern    = SearchSnippets.exactPattern(input, ignoreCase);
		final String luceneQuery = exactLuceneQuery(input);

		if (pattern == null || luceneQuery == null) {

			return List.of();
		}

		final Map<String, FileHits> files = new LinkedHashMap<>();

		for (final ChunkHit hit : chunkHits(securityContext, luceneQuery, options, MAX_FETCH)) {

			final ExtractedContentNode chunk   = hit.chunk();
			final List<SearchSnippet> snippets = new ArrayList<>();
			final FileHits known               = files.get(hit.file().getUuid());
			final int missing                  = maxSnippets - (known != null ? known.snippets().size() : 0);

			snippets.addAll(SearchSnippets.find(chunk.getText(), pattern, chunk.getPageStart(), contextChars, Math.max(missing, 1)));

			// the index covers the name of the embedded document (archive entry, attachment) as well
			if (snippets.isEmpty() && chunk.getEmbeddedPath() != null) {

				snippets.addAll(SearchSnippets.find(chunk.getEmbeddedPath(), pattern, chunk.getPageStart(), contextChars, 1));
			}

			if (snippets.isEmpty()) {

				continue;
			}

			final FileHits fileHits = files.computeIfAbsent(hit.file().getUuid(), uuid -> new FileHits(hit.file(), hit.score(), new HashSet<>(), new ArrayList<>()));

			if (missing > 0) {

				fileHits.snippets().addAll(snippets.subList(0, Math.min(missing, snippets.size())));
			}
		}

		return files.values().stream()
			.limit(options.limit())
			.map(f -> new FileSearchHit(f.file(), f.score(), List.copyOf(f.snippets())))
			.toList();
	}

	/**
	 * The search hit for a chunk found by other means, e.g. by a vector index, or null if the security
	 * context may not read it or the options filter it out. Uses minScore, fileUuids, folder and
	 * contextChunks of the options, so hits from other searches are filtered exactly like keyword hits.
	 */
	public static ContentSearchHit searchHit(final SecurityContext securityContext, final NodeInterface node, final double score, final ContentSearchOptions options) {

		final ChunkHit hit = chunkHit(securityContext, node, score, options, folderPrefix(options));

		return hit != null ? searchHit(hit, options) : null;
	}

	// ----- private methods -----
	private record ChunkHit(ExtractedContentNode chunk, double score, File file) {
	}

	private record FileHits(File file, double score, Set<Integer> matchedTerms, List<SearchSnippet> snippets) {
	}

	private static String luceneQuery(final String query, final ContentSearchMode mode) {

		if (mode == ContentSearchMode.SIMPLE) {

			return ContentSearchQuery.parse(query).toLucene();
		}

		return query == null || query.isBlank() ? null : query;
	}

	/**
	 * The chunks that may contain the input: every word of it, also inside a longer indexed term, since the
	 * analyzer keeps e.g. "invoices.sendreminder" together. Words shorter than 3 characters are left out
	 * when there are longer ones, as a leading wildcard on them matches most of the term dictionary; the
	 * text check finds them anyway. Null if the input holds no word.
	 */
	private static String exactLuceneQuery(final String input) {

		final List<String> longWords  = new ArrayList<>();
		final List<String> shortWords = new ArrayList<>();

		if (input != null) {

			final Matcher matcher = WORD.matcher(input);

			while (matcher.find()) {

				final String word = "*" + matcher.group().toLowerCase(Locale.ROOT) + "*";

				(matcher.group().length() >= MIN_EXACT_WORD_LENGTH ? longWords : shortWords).add(word);
			}
		}

		final List<String> clauses = longWords.isEmpty() ? shortWords : longWords;

		return clauses.isEmpty() ? null : String.join(" AND ", clauses);
	}

	/**
	 * Chunk hits from the fulltext index, best first, without chunks the security context may not read or
	 * the options filter out.
	 */
	private static List<ChunkHit> chunkHits(final SecurityContext securityContext, final String luceneQuery, final ContentSearchOptions options, final int fetch) throws FrameworkException {

		final int pageSize                     = Math.min(MAX_FETCH, Math.max(fetch, 50));
		final Map<NodeInterface, Double> found = StructrApp.getInstance(securityContext).getNodesFromFulltextIndex(ContentExtractionTraits.CONTENT_FULLTEXT_INDEX, luceneQuery, 1, pageSize);
		final String folderPrefix              = folderPrefix(options);
		final List<ChunkHit> hits              = new ArrayList<>();

		for (final Map.Entry<NodeInterface, Double> entry : found.entrySet()) {

			final ChunkHit hit = chunkHit(securityContext, entry.getKey(), entry.getValue() != null ? entry.getValue() : 0.0, options, folderPrefix);
			if (hit != null) {

				hits.add(hit);
			}
		}

		return hits;
	}

	/**
	 * The hit for a node, or null if it is no chunk, the security context may not read it or the options
	 * filter it out.
	 */
	private static ChunkHit chunkHit(final SecurityContext securityContext, final NodeInterface node, final double score, final ContentSearchOptions options, final String folderPrefix) {

		if (node == null || score < options.minScore() || !node.is(ContentExtractionTraits.EXTRACTED_CONTENT_NODE) || !securityContext.isVisible(node)) {

			return null;
		}

		final ExtractedContentNode chunk = node.as(ExtractedContentNode.class);
		final File file                  = chunk.getFile();

		if (file == null || (options.fileUuids() != null && !options.fileUuids().contains(file.getUuid())) || (folderPrefix != null && !file.getPath().startsWith(folderPrefix))) {

			return null;
		}

		return new ChunkHit(chunk, score, file);
	}

	private static ContentSearchHit searchHit(final ChunkHit hit, final ContentSearchOptions options) {

		final String context = options.contextChunks() > 0 ? ContentChunkReader.context(hit.chunk(), options.contextChunks(), options.contextChunks()).text() : null;

		return new ContentSearchHit(hit.chunk(), hit.score(), hit.file(), context);
	}

	private static String folderPrefix(final ContentSearchOptions options) {

		return options.folder() != null ? options.folder().getPath() + "/" : null;
	}
}
