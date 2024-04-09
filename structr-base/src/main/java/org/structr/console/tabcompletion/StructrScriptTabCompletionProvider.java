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

import org.structr.common.SecurityContext;
import org.structr.core.function.Functions;

import java.util.*;

/**
 *
 */
public class StructrScriptTabCompletionProvider extends AbstractTabCompletionProvider {

	private final Set<String> words = new TreeSet<>();

	public StructrScriptTabCompletionProvider() {

		words.add("me");
		words.add("page");
		words.add("request");
		words.add("this");
	}


	@Override
	public List<TabCompletionResult> getTabCompletion(final SecurityContext securityContext, final String line) {

		final List<TabCompletionResult> results = new LinkedList<>();
		final String token                      = getToken(line, " ()[]{}=+-,.*#/&$!");

		results.addAll(getExactResultsForCollection(Functions.getNames(), token, "("));
		results.addAll(getExactResultsForCollection(words, token, ""));

		Collections.sort(results);

		return results;
	}
}
