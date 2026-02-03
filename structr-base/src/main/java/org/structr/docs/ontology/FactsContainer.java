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
package org.structr.docs.ontology;

import org.structr.core.function.tokenizer.FactsTokenizer;
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.ontology.parser.token.AbstractToken;
import org.structr.docs.ontology.parser.token.UnresolvedToken;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public abstract class FactsContainer {

	public abstract List<Token> getTokens();
	public abstract String getName();
	public abstract void writeToDisc();

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		for (Token token : getTokens()) {

			buf.append(token.getRawContent());
		}

		return buf.toString();
	}

	public Deque<AbstractToken> getFilteredTokens() {

		final Deque<AbstractToken> result = new LinkedList<>();

		for (final Token token : getTokens()) {

			if (token.isNotBlank() && !token.isComment()) {

				result.add(new UnresolvedToken(token));
			}
		}

		return result;
	}

	public List<Token> insertAfter(final Token reference, String text) {

		final List<Token> newTokens = tokenize(text);
		final List<Token> tokens    = getTokens();
		final int index             = tokens.indexOf(reference);

		if (index >= 0) {

			tokens.addAll(index + 1, newTokens);

			return newTokens;
		}

		return null;
	}

	// ----- protected methods -----
	protected List<Token> tokenize(final String text) {
		return new FactsTokenizer().tokenize(this, text);
	}

	public void remove(final Token token) {
		getTokens().remove(token);
	}
}
