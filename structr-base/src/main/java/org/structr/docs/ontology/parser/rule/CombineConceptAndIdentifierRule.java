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
package org.structr.docs.ontology.parser.rule;

import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Ontology;
import org.structr.docs.ontology.parser.token.*;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiFunction;

public class CombineConceptAndIdentifierRule extends Rule {

	private final Map<ConceptType, BiFunction<ConceptToken, IdentifierToken, Token>> SpecializedTokens = Map.of(
		ConceptType.Blacklist,      DoBlacklistToken::new,
		ConceptType.MarkdownFolder, MarkdownFolderToken::new,
		ConceptType.MarkdownFile,   MarkdownFileToken::new,
		ConceptType.JavascriptFile, JavascriptFileToken::new,
		ConceptType.CodeSource,     CodeSourceToken::new,
		ConceptType.EnumSource,     EnumSourceToken::new
	);

	public CombineConceptAndIdentifierRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<Token> tokens) {

		final Deque<Token> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final Token token1 = tokens.pop();

			if (token1 instanceof IdentifierToken identifierToken && !tokens.isEmpty()) {

				// unresolved => check if next is existing concept
				final Token token2 = tokens.pop();

				if (token2 instanceof ConceptToken conceptToken) {

					final ConceptType type = conceptToken.getType();

					if (SpecializedTokens.containsKey(type)) {

						result.add(SpecializedTokens.get(type).apply(conceptToken, identifierToken));

					} else {

						result.add(new NamedConceptToken(conceptToken, identifierToken));
					}

				} else {

					// move both tokens to result without changing anything
					result.add(token1);
					result.add(token2);
				}

			} else if (token1 instanceof ConceptToken conceptToken && !tokens.isEmpty()) {

				// concept => check if next is unresolved
				final Token token2 = tokens.pop();

				if (token2 instanceof IdentifierToken identifierToken) {

					final ConceptType type = conceptToken.getType();

					if (SpecializedTokens.containsKey(type)) {

						result.add(SpecializedTokens.get(type).apply(conceptToken, identifierToken));

					} else {

						result.add(new NamedConceptToken(conceptToken, identifierToken));
					}

				} else {

					// move both tokens to result without changing anything
					result.add(conceptToken);
					result.add(token2);
				}

			} else {

				// move token to result without changing anything
				result.add(token1);
			}
		}

		// restore input
		tokens.addAll(result);
	}
}
