/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.RelationshipInterface;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SearchRelationshipsFulltextFunction extends CoreFunction implements QueryFunction {

	public static final String ERROR_MESSAGE_SEARCH    = "Usage: ${search_relationships_fulltext(indexName, searchString)}. Example: ${search_relationships_fulltext(\"User_name\", \"abc\")}";
	public static final String ERROR_MESSAGE_SEARCH_JS = "Usage: ${{Structr.searchRelationshipsFulltext(indexName, value)}}. Example: ${{Structr.searchRelationshipsFulltext(\"User_name\", \"abc\")}}";

	@Override
	public String getName() {
		return "search_relationships_fulltext";
	}

	@Override
	public String getSignature() {
		return "indexName, searchString";
	}

	@Override
	public String getNamespaceIdentifier() {
		return "searchRelationshipsFulltext";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();

		try {

			if (sources == null) {

				throw new IllegalArgumentException();
			}

			assertArrayHasLengthAndTypes(sources, 2, String.class, String.class);

			final String indexName                          = sources[0].toString();
			final String searchString                       = sources[1].toString();
			final Map<RelationshipInterface, Double> result = StructrApp.getInstance(securityContext).getRelationshipsFromFulltextIndex(indexName, searchString, 10, 1);
			final List<Map<String, Object>> list            = new LinkedList<>();

			for (final Map.Entry<RelationshipInterface, Double> entry : result.entrySet()) {

				list.add(Map.of("relationship", entry.getKey(), "score", entry.getValue()));
			}

			return list;

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SEARCH_JS : ERROR_MESSAGE_SEARCH);
	}

	@Override
	public String shortDescription() {
		return "Returns a map of entities and search scores matching the given search string from the given fulltext index. Searches case-insensitve / inexact.";
	}
}
