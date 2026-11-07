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
package org.structr.extraction.function.graph;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.search.ContentSearchHit;
import org.structr.extraction.api.search.ContentSearchMode;
import org.structr.extraction.api.search.ContentSearchOptions;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.Folder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DocumentsFulltextSearchFunction extends ContentExtractionFunction {

	private static final String FILES          = "files";
	private static final String FOLDER         = "folder";
	private static final String LIMIT          = "limit";
	private static final String MIN_SCORE      = "minScore";
	private static final String CONTEXT_CHUNKS = "contextChunks";
	private static final String MODE           = "mode";

	private static final Set<String> ACCEPTED = Set.of(FILES, FOLDER, LIMIT, MIN_SCORE, CONTEXT_CHUNKS, MODE);

	@Override
	public String getName() {

		return NAMESPACE + ".fulltextSearch";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("query [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final String query                = textArgument(sources, 0);
		final Map<String, Object> options = optionsArgument(sources, 1, ACCEPTED);
		final int limit                   = options.containsKey(LIMIT) ? positiveIntOption(options, LIMIT) : 10;
		final int contextChunks           = options.containsKey(CONTEXT_CHUNKS) ? nonNegativeIntOption(options, CONTEXT_CHUNKS) : 0;
		final double minScore             = minScore(options);
		final Set<String> files           = files(options);
		final Folder folder               = folder(options);
		final ContentSearchMode mode      = mode(options);
		final List<ContentSearchHit> hits = ContentExtraction.search(ctx.getSecurityContext(), query, new ContentSearchOptions(limit, minScore, files, folder, contextChunks, mode));

		return toGraphObjectMaps(hits.stream().map(ContentSearchHit::toMap).toList());
	}

	@Override
	public String getShortDescription() {

		return "Searches the text extracted from files by words (fulltext search).";
	}

	@Override
	public String getLongDescription() {

		return """
			Searches only the text that content extraction stored for files (the chunks in `ExtractedContentNode`s), not the attributes of \
			your own types; for those, use `searchFulltext()` with the fulltext index of an attribute.

			Searches the chunks through their fulltext index and returns the best hits as objects with `node`, `score`, \
			`text`, `file`, `position`, `pageStart`, `pageEnd`, `headingPath`, `embeddedPath` (the archive entry or attachment the chunk is in), \
			`figure` (the `DocumentFigure` whose text the chunk holds, or null; show its image by its id), \
			and with `contextChunks` also `context`, the hit together with its neighbouring chunks in the same embedded document. The index \
			covers `embeddedPath` as well, so a chunk is also found by the name of the archive entry or attachment it is in. By default the query uses Lucene syntax: words, "quoted phrases", AND/OR, wildcards. With `mode: 'simple'` \
			it is read like the input of a search box instead: every word must occur, as a prefix or with small typos, ignoring case.

			Only chunks of files the current user can read are found. To find text by meaning rather than by words, use \
			`documents.vectorSearch()` (Structr Enterprise). The article *Search & Retrieval* (Building Applications) explains which search function \
			to use when.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("query", "the search query, in Lucene syntax"),
			Parameter.optional("options", """
				an object with any of: `limit` (maximum number of hits, default 10), `minScore`, `files` (a list of Files to search in), `folder` \
				(a Folder: search files in it and its subfolders), `contextChunks` (number of neighbouring chunks to include on each side), \
				`mode` (`lucene`, the default, or `simple`)"""));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.javaScript("""
			${{
			    let manuals = $.first($.find('Folder', { name: 'manuals' }));

			    return $.documents.fulltextSearch('"reset password"', { folder: manuals, limit: 5, contextChunks: 1 })
			        .map(hit => hit.file.name + ', page ' + hit.pageStart + ': ' + hit.context);
			}}
			""", "Search manuals and show where the hits are"));
	}

	@Override
	public List<String> getNotes() {

		return List.of("The fulltext index on `ExtractedContentNode.text` is created by Structr's schema index management, like every other index.");
	}

	// ----- private methods -----
	private double minScore(final Map<String, Object> options) throws FrameworkException {

		final Object value = options.get(MIN_SCORE);
		if (value == null) {

			return 0.0;
		}

		if (value instanceof Number number) {

			return number.doubleValue();
		}

		throw new FrameworkException(422, getDisplayName(false) + ": option 'minScore' must be a number.");
	}

	private Set<String> files(final Map<String, Object> options) throws FrameworkException {

		final Object value = options.get(FILES);
		if (value == null) {

			return null;
		}

		final Set<String> uuids = new HashSet<>();

		if (value instanceof Iterable<?> iterable) {

			for (final Object o : iterable) {

				if (o instanceof NodeInterface node && node.is(StructrTraits.FILE)) {

					uuids.add(node.getUuid());

				} else {

					throw new FrameworkException(422, getDisplayName(false) + ": option 'files' must be a list of Files.");
				}
			}

			return uuids;
		}

		throw new FrameworkException(422, getDisplayName(false) + ": option 'files' must be a list of Files.");
	}

	private ContentSearchMode mode(final Map<String, Object> options) throws FrameworkException {

		if (!options.containsKey(MODE)) {

			return ContentSearchMode.LUCENE;
		}

		try {

			return ContentSearchMode.of(stringOption(options, MODE));

		} catch (final IllegalArgumentException ex) {

			throw new FrameworkException(422, getDisplayName(false) + ": option 'mode': " + ex.getMessage());
		}
	}

	private Folder folder(final Map<String, Object> options) throws FrameworkException {

		final Object value = options.get(FOLDER);
		if (value == null) {

			return null;
		}

		if (value instanceof NodeInterface node && node.is(StructrTraits.FOLDER)) {

			return node.as(Folder.class);
		}

		throw new FrameworkException(422, getDisplayName(false) + ": option 'folder' must be a Folder.");
	}
}
