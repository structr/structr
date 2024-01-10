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

import java.util.*;

/**
 *
 */
public abstract class AbstractTabCompletionProvider implements TabCompletionProvider {

	protected List<TabCompletionResult> getCaseInsensitiveResultsForCollection(final Collection<String> words, final String token, final String suffix) {

		final List<TabCompletionResult> result = new LinkedList<>();
		final String lowerToken                = token.toLowerCase();
		final boolean upperCase                = StringUtils.isAllUpperCase(token);

		for (final String word : words) {

			if (word.startsWith(lowerToken)) {

				if (upperCase) {

					result.add(getCompletion(word.toUpperCase(), token));

				} else {

					result.add(getCompletion(word, token));
				}
			}
		}

		return result;
	}

	protected List<TabCompletionResult> getExactResultsForCollection(final Collection<String> words, final String token, final String suffix) {

		final List<TabCompletionResult> result = new LinkedList<>();

		for (final String word : words) {

			if (word.startsWith(token)) {

				result.add(getCompletion(word, token, suffix));
			}
		}

		return result;
	}

	protected TabCompletionResult getCompletion(final String command, final String token) {
		return getCompletion(command, token, " ");
	}

	protected TabCompletionResult getCompletion(final String command, final String token, final String suffix) {
		return new TabCompletionResult(command, command.substring(token.length()), suffix);
	}

	protected String getToken(final String line, final String separators) {

		final String[] parts = StringUtils.splitPreserveAllTokens(line, separators);
		if (parts.length > 0) {

			return parts[parts.length-1];
		}

		return "";
	}

	protected Map<String, Object> toMap(final String key, final Object value) {
		return toMap(new HashMap<>(), key, value);
	}

	protected Map<String, Object> toMap(final Map<String, Object> map, final String key, final Object value) {

		Map thisMap = map;
		if (thisMap == null) {

			thisMap = new HashMap<>();
		}

		if (key != null && value != null) {
			thisMap.put(key, value);
		}

		return thisMap;
	}
}
