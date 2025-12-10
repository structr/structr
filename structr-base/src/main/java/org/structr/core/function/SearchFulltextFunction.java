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
import org.structr.docs.Example;
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
			Usage.structrScript("Usage: ${searchFulltext(indexName, searchString)}. Example: ${searchFulltext(\"index_name\", \"abc\")}"),
			Usage.javaScript("Usage: ${{$.searchFulltext(indexName, searchString)}}. Example: ${{$.searchFulltext(\"index_name\", \"abc\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a map of entities and search scores matching the given search string from the given fulltext index.";
	}

	@Override
	public String getLongDescription() {
		return """
			Searches are **case-insensitive**. A query term matches only if the **complete token** exists in the fulltext index; partial substrings inside a longer token won't match unless that exact token is indexed.
			
			For type attributes with the fulltext index flag enabled, the fulltext index name is auto-generated as `<type name>_<attribute name>_fulltext` (for example, `Employee_firstName_fulltext`).
			
			Supported wildcards:
			- `?` matches exactly one character
			- `*` matches zero or more characters
			
			For performance reasons, avoid placing `*` at the beginning of a term (for example `*test`), because leading wildcards can make queries much more expensive. Prefer anchored patterns like `test*` where possible.
			
			Supported query modifiers:
			- `~` enables fuzzy matching for the preceding term, allowing similar words rather than exact equality.
			""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
				${{
					$.searchFulltext("Employee_title_fulltext", "Senior*");
				}}
				""", "Wildcard fulltext search for all employees with a senior role"),
				Example.javaScript("""
				${{
					$.searchFulltext("Employee_firstName_fulltext", "alex~");
				}}
				""", "Fuzzy fulltext search for all employees with names similar to 'alex'")
		);
	}
}
