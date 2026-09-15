/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class JavaScriptTabCompletionProvider extends AbstractTabCompletionProvider {

	@Override
	public List<TabCompletionResult> getTabCompletion(final SecurityContext securityContext, final String line) {

		final List<TabCompletionResult> results = new LinkedList<>();
		final Matcher structrCallPatternMatcher = Pattern.compile(".*(Structr|\\$)\\.([A-Za-z0-9]+)$").matcher(line);

		if (structrCallPatternMatcher.matches()) {

			final String callStylePrefix  = structrCallPatternMatcher.group(1);
			final String completionPrefix = structrCallPatternMatcher.group(2);
			final List<TabCompletionResult> intermediateList = getExactResultsForCollection(Functions.getNames(), completionPrefix, "(");

			intermediateList.addAll(getExactResultsForCollection(SchemaMethodTraitDefinition.getKeywordNames(), completionPrefix, " "));
			intermediateList.addAll(getExactResultsForCollection(Traits.getAllTypes(Traits::isNodeType), completionPrefix, "."));

			intermediateList.forEach((tcr) -> {
				results.add(new TabCompletionResult(callStylePrefix + "." + tcr.getCommand(), tcr.getCompletion(), tcr.getSuffix()));
			});
		}

		Collections.sort(results);

		return results;
	}
}
