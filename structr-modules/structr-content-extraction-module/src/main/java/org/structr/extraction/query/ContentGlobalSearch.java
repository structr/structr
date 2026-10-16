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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.GlobalSearchContributor;
import org.structr.core.traits.StructrTraits;
import org.structr.extraction.api.search.ContentSearchMode;
import org.structr.extraction.api.search.ContentSearchOptions;
import org.structr.extraction.api.search.FileSearchHit;
import org.structr.extraction.api.search.SearchSnippet;
import org.structr.web.entity.File;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds files whose extracted content matches to the global search of the admin UI ("files" context):
 * one result per file, under the key "extractedContent", with the first match in context. The search
 * goes through the fulltext index like the search box of the Files area, so it always ignores case.
 */
public final class ContentGlobalSearch implements GlobalSearchContributor {

	public static final ContentGlobalSearch INSTANCE = new ContentGlobalSearch();

	/** The key the admin UI shows as "content". */
	public static final String RESULT_KEY = "extractedContent";

	private static final Logger logger     = LoggerFactory.getLogger(ContentGlobalSearch.class);
	private static final int MAX_FILES     = 100;
	private static final int CONTEXT_CHARS = 24;

	private ContentGlobalSearch() {
	}

	@Override
	public List<Map<String, Object>> search(final SecurityContext securityContext, final Set<String> searchContexts, final String searchString, final boolean searchIgnoreCase) {

		final List<Map<String, Object>> results = new ArrayList<>();

		if (!searchContexts.contains(DatabaseService.GLOBAL_SEARCH_CONTEXT_FILES)) {

			return results;
		}

		final ContentSearchOptions options = new ContentSearchOptions(MAX_FILES, 0.0, null, null, 0, ContentSearchMode.SIMPLE);

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			for (final FileSearchHit hit : ContentKeywordSearch.searchFiles(securityContext, searchString, options, 1, CONTEXT_CHARS)) {

				if (!hit.snippets().isEmpty()) {

					results.add(result(hit.file(), hit.snippets().get(0)));
				}
			}

			tx.success();

		} catch (final FrameworkException ex) {

			logger.warn("Unable to search extracted content: {}", ex.getMessage());
		}

		return results;
	}

	// ----- private methods -----
	private static Map<String, Object> result(final File file, final SearchSnippet snippet) {

		final Map<String, Object> value  = new LinkedHashMap<>();
		final Map<String, Object> result = new LinkedHashMap<>();

		value.put("before", snippet.before());
		value.put("match",  snippet.match());
		value.put("after",  snippet.after());

		result.put("id",     file.getUuid());
		result.put("type",   file.getType());
		result.put("name",   file.getName());
		result.put("keys",   List.of(RESULT_KEY));
		result.put("values", List.of(value));
		result.put("labels", List.of(StructrTraits.ABSTRACT_FILE, StructrTraits.FILE, file.getType()));

		return result;
	}
}
