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
import org.structr.core.traits.StructrTraits;
import org.structr.extraction.api.search.ContentSearchMode;
import org.structr.extraction.api.search.ContentSearchOptions;
import org.structr.extraction.api.search.FileSearchHit;
import org.structr.extraction.api.search.SearchSnippet;
import org.structr.extraction.function.ResultMaps;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backs the static method ExtractedContentNode.searchFiles, which the search box of the Files area calls
 * ({@code POST /structr/rest/ExtractedContentNode/searchFiles} with {@code { query, folder, limit }}).
 */
public final class ContentFileSearch {

	public static final int DEFAULT_LIMIT  = 50;
	public static final int MAX_LIMIT      = 200;
	public static final int SNIPPETS       = 5;
	public static final int CONTEXT_CHARS  = 120;

	private ContentFileSearch() {
	}

	/**
	 * Files whose content matches the query, as maps with the file's id, type, name and path, the score
	 * and the snippets.
	 *
	 * @param queryArgument  what the user typed
	 * @param folderArgument a Folder or its UUID to search below; null for all files
	 * @param limitArgument  maximum number of files; null for the default
	 */
	public static Object search(final SecurityContext securityContext, final Object queryArgument, final Object folderArgument, final Object limitArgument) throws FrameworkException {

		if (!(queryArgument instanceof String query)) {

			throw new FrameworkException(422, "Expected the search text as query.");
		}

		final ContentSearchOptions options = new ContentSearchOptions(limit(limitArgument), 0.0, null, folder(securityContext, folderArgument), 0, ContentSearchMode.SIMPLE);
		final List<FileSearchHit> hits     = ContentKeywordSearch.searchFiles(securityContext, query, options, SNIPPETS, CONTEXT_CHARS);

		return hits.stream().map(hit -> ResultMaps.toGraphObjectMap(toMap(hit))).toList();
	}

	// ----- private methods -----
	private static Map<String, Object> toMap(final FileSearchHit hit) {

		final File file                = hit.file();
		final Map<String, Object> map  = new LinkedHashMap<>();
		final Map<String, Object> info = new LinkedHashMap<>();

		info.put("id",   file.getUuid());
		info.put("type", file.getType());
		info.put("name", file.getName());
		info.put("path", file.getPath());

		map.put("file",     info);
		map.put("score",    hit.score());
		map.put("snippets", hit.snippets().stream().map(SearchSnippet::toMap).toList());

		return map;
	}

	private static int limit(final Object argument) throws FrameworkException {

		if (argument == null) {

			return DEFAULT_LIMIT;
		}

		if (argument instanceof Number number && number.intValue() > 0) {

			return Math.min(number.intValue(), MAX_LIMIT);
		}

		throw new FrameworkException(422, "Expected a positive number as limit.");
	}

	private static Folder folder(final SecurityContext securityContext, final Object argument) throws FrameworkException {

		final NodeInterface node;

		if (argument == null || (argument instanceof String text && text.isBlank())) {

			return null;

		} else if (argument instanceof NodeInterface given) {

			node = given;

		} else if (argument instanceof String uuid) {

			node = StructrApp.getInstance(securityContext).getNodeById(StructrTraits.FOLDER, uuid);

		} else {

			throw new FrameworkException(422, "Expected a Folder (or its UUID) as folder.");
		}

		if (node == null || !node.is(StructrTraits.FOLDER)) {

			throw new FrameworkException(404, "Folder " + argument + " not found.");
		}

		return node.as(Folder.class);
	}
}
