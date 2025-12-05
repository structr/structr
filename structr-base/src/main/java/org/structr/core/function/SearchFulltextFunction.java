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

import org.apache.tika.utils.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SearchFulltextFunction extends CoreFunction implements QueryFunction {

	@Override
	public String getName() {
		return "searchFulltext";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("indexName, searchString");
	}

	@Override
	public String getNamespaceIdentifier() {
		return "searchFulltext";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();

		try {

			if (sources == null) {

				throw new IllegalArgumentException();
			}

			assertArrayHasLengthAndTypes(sources, 2, String.class, String.class);

			final String indexName                  = sources[0].toString();
			final String searchString               = sources[1].toString();

			if (StringUtils.isBlank(indexName)) {
				throw new FrameworkException(422, "Argument indexName must not be empty.");
			}

			if (StringUtils.isBlank(searchString)) {
				throw new FrameworkException(422, "Argument searchString must not be empty.");
			}

			final Map<NodeInterface, Double> result = StructrApp.getInstance(securityContext).getNodesFromFulltextIndex(indexName, searchString, 10, 1);
			final List<Map<String, Object>> list    = new LinkedList<>();

			for (final Map.Entry<NodeInterface, Double> entry : result.entrySet()) {

				list.add(Map.of("node", entry.getKey(), "score", entry.getValue()));
			}

			return list;

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${searchFulltext(indexName, searchString)}. Example: ${searchFulltext(\"UserName\", \"abc\")}"),
			Usage.javaScript("Usage: ${{Structr.searchFulltext(indexName, value)}}. Example: ${{Structr.searchFulltext(\"UserName\", \"abc\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a map of entities and search scores matching the given search string from the given fulltext index. Searches case-insensitve / inexact.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
