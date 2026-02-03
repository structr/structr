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
package org.structr.core.function.tokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.docs.ontology.FactsContainer;

import java.util.LinkedList;
import java.util.List;

public class FactsTokenizer {

	private static final Logger logger       = LoggerFactory.getLogger(FactsTokenizer.class);
	private final List<Tokenizer> candidates = new LinkedList<>();
	private List<Token> tokens               = new LinkedList<>();
	private Tokenizer currentToken           = null;
	private int column                       = 1;
	private int row                          = 1;
	private boolean isSilent                 = false;

	public FactsTokenizer() {

		candidates.add(new IdentifierWithPlus());
		candidates.add(new HashComment());
		candidates.add(new MultipleCharacters((char) 9));  // tab
		candidates.add(new MultipleCharacters((char) 10)); // newline
		candidates.add(new MultipleCharacters((char) 13)); // carriage-return
		candidates.add(new MultipleCharacters((char) 32)); // space
		candidates.add(new SingleCharacter(','));
		candidates.add(new SingleCharacter(';'));
		candidates.add(new SingleCharacter(':'));
		candidates.add(new SingleCharacter('('));
		candidates.add(new SingleCharacter(')'));
		candidates.add(new SingleCharacter('['));
		candidates.add(new SingleCharacter(']'));
		candidates.add(new SingleCharacter('{'));
		candidates.add(new SingleCharacter('}'));
		candidates.add(new SingleCharacter('*'));
		candidates.add(new SingleCharacter('$'));
		candidates.add(new SingleCharacter('|'));

		candidates.add(new QuotedString('\'', false));
		candidates.add(new QuotedString('\"', false));
		candidates.add(new Number());
	}

	public List<Token> tokenize(final FactsContainer factsContainer, final String expression) {

		final char[] chars = expression.toCharArray();
		final int length   = chars.length;
		int count          = 0;
		int i              = 0;

		while (i < length && count++ < 100000) {

			while (i < length && currentToken != null && currentToken.accept(chars[i])) {

				currentToken.add(chars[i]);

				if (chars[i] == '\n') {
					column = 1;
					row++;
				} else {
					column++;
				}

				i++;
			}

			// find token for character
			if (i < length) {

				final Tokenizer nextToken = findToken(chars[i]);
				if (nextToken != null) {

					if (currentToken != null) {

						tokens.add(currentToken.getToken(factsContainer));
					}

					currentToken = nextToken;

				} else {

					if (!isSilent) {
						logger.warn("Unexpected character {} ({}) in string \"{}\". Tokens: {}", (int) chars[i], Character.toString(chars[i]), expression, tokens);
					}

					// no token, stop parsing
					break;
				}
			}
		}

		if (currentToken != null) {

			tokens.add(currentToken.getToken(factsContainer));
		}

		return tokens;
	}

	private Tokenizer findToken(final char c) {

		for (final Tokenizer t : candidates) {

			final Tokenizer instance = t.newInstance();
			if (instance.accept(c)) {

				instance.init(row, column);

				return instance;
			}
		}

		return null;
	}

	public void setIsSilent(final boolean isSilent) {
		this.isSilent = isSilent;
	}
}
