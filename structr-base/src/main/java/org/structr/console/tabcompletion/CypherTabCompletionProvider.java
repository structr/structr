/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.console.tabcompletion;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class CypherTabCompletionProvider extends AbstractTabCompletionProvider {

	private static final Pattern nodePattern = Pattern.compile("\\([a-zA-Z]?[a-zA-Z0-9]+:[a-zA-Z]?[a-zA-Z0-9]+");
	private static final Pattern relPattern  = Pattern.compile("\\[[a-zA-Z]?[a-zA-Z0-9]+:[a-zA-Z]?[a-zA-Z0-9]+");
	private final List<String> words         = new LinkedList<>();

	public CypherTabCompletionProvider() {

		words.add("any");
		words.add("asc");
		words.add("assert");
		//words.add("by");
		words.add("case");
		words.add("constraint");
		words.add("create");
		words.add("csv");
		words.add("delete");
		words.add("desc");
		words.add("detach");
		words.add("distinct");
		words.add("drop");
		words.add("end");
		words.add("foreach");
		words.add("from");
		//words.add("in");
		words.add("index");
		words.add("limit");
		words.add("load");
		words.add("match");
		words.add("merge");
		//words.add("on");
		words.add("order");
		words.add("remove");
		words.add("return");
		words.add("set");
		words.add("skip");
		words.add("then");
		words.add("union");
		words.add("unique");
		words.add("unwind");
		words.add("using");
		words.add("when");
		words.add("where");
		words.add("with");

	}

	@Override
	public List<TabCompletionResult> getTabCompletion(final SecurityContext securityContext, final String line) {

		final List<TabCompletionResult> results = new LinkedList<>();
		final String token                      = getToken(line, " ");

		results.addAll(getCaseInsensitiveResultsForCollection(words, token, " "));

		// node type results
		final Matcher nodeMatcher = nodePattern.matcher(token);
		if (nodeMatcher.matches()) {

			final Set<String> keys = getNodeTypes();
			final String subtoken  = StringUtils.substringAfterLast(token, ":");
			final String suffix    = "";

			results.addAll(getExactResultsForCollection(keys, subtoken, suffix));
		}

		/*
		 * disabled, not possible to get a runtime list of relationship types yet
		 *
		 *
		final Matcher relMatcher = relPattern.matcher(token);
		if (relMatcher.matches()) {

			final Set<String> keys = getRelationshipTypes();
			final String subtoken  = StringUtils.substringAfterLast(token, ":");
			final String suffix    = "";

			results.addAll(getExactResultsForCollection(keys, subtoken, suffix));
		}
		 */

		Collections.sort(results);

		return results;
	}

	private Set<String> getNodeTypes() {
		return StructrApp.getConfiguration().getNodeEntities().keySet();
	}
}
