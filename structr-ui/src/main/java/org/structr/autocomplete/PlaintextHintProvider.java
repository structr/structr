/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.autocomplete;

import java.util.ArrayList;
import java.util.List;
import org.structr.core.GraphObject;

/**
 *
 *
 */
public class PlaintextHintProvider extends AbstractHintProvider {

	@Override
	protected String getFunctionName(String sourceName) {
		return sourceName;
	}

	@Override
	public List<GraphObject> getHints(final GraphObject currentEntity, final String type, final String mainToken, final String secondaryToken, final String otherToken, final int line, final int cursorPosition) {

		final List<String> tokens     = parseTokens(mainToken, cursorPosition);
		final String currentToken     = getTokenOrBlank(tokens, 0);
		final String previousToken    = getTokenOrBlank(tokens, 1);
		final String thirdToken       = getTokenOrBlank(tokens, 2);

		return super.getHints(currentEntity, type, currentToken, previousToken, thirdToken, line, cursorPosition);
	}

	private List<String> parseTokens(final String source, final int cursorPosition) {

		final int length            = cursorPosition != -1 && cursorPosition < source.length() ? cursorPosition : source.length();
		final List<String> tokens   = new ArrayList<>();
		final StringBuilder buf     = new StringBuilder();
		boolean currentIsLetter     = false;
		boolean lastWasLetter       = false;

		for (int i=0; i<length; i++) {

			final char c    = source.charAt(i);
			lastWasLetter   = currentIsLetter;
			currentIsLetter = Character.isDigit(c) || Character.isLetter(c) || c == '_';

			if (lastWasLetter != currentIsLetter) {
				tokens.add(buf.toString().trim());
				buf.setLength(0);
			}

			buf.append(c);
		}

		tokens.add(buf.toString().trim());

		// remove single point (".") tokens between two strings
		int len = tokens.size();
		for (int i=1; i<len; i++) {

			final String token  = tokens.get(i);
			final String before = tokens.get(i-1);

			if (i+1 < len) {

				final String after  = tokens.get(i+1);
				if (".".equals(token) && !".".equals(before) && !".".equals(after)) {

					tokens.remove(i);
					len -= 1;
				}
			}
		}

		return tokens;
	}

	@Override
	protected boolean isJavascript() {
		return false;
	}

	// ----- private methods -----
	private String getTokenOrBlank(final List<String> tokens, final int reverseIndex) {

		final int length = tokens.size();
		final int index  = length - reverseIndex - 1;

		if (index >= 0 && length > 0 && index < length) {
			return tokens.get(index);
		}

		return "";
	}
}
