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
package org.structr.docs.ontology.parser.rule;

import org.structr.docs.ontology.Ontology;
import org.structr.docs.ontology.parser.token.*;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class IdentifyListsRule extends Rule {

	public IdentifyListsRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final List<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken token1 = tokens.pop();

			if (token1 instanceof IdentifierListToken list) {

				final AbstractToken token2 = tokens.pop();

				// example: one, two and three

				if (token2 instanceof ConjunctionToken && !tokens.isEmpty()) {

					final AbstractToken token3 = tokens.pop();

					if (token3 instanceof IdentifierToken identifier3) {

						list.addToken(identifier3);

						// include syntactical tokens so we can edit the text later on
						identifier3.addSyntaxToken(token2.getToken());

						tokens.push(list);


					} else if (token3 instanceof ConceptToken concept1) {

						// If we arrive here, there was a known concept in the list
						// that should most likely be handled as an identifier, hence
						// we convert it back to an identifier token.

						final IdentifierToken id = concept1.asIdentifierToken();

						// include syntactical tokens so we can edit the text later on
						id.addSyntaxToken(token2.getToken());

						list.addToken(id);

						tokens.push(list);

					} else {

						throw new RuntimeException("Syntax error: expected identifier in list, got " + token3 + ". Compound phrases are not supported, please use separate phrases to specify facts with different concepts.");
					}

				} else {

					// token1 is processed
					result.add(token1);

					// token2 needs to stay
					tokens.push(token2);
				}

			} else if (token1 instanceof IdentifierToken identifier1 && !tokens.isEmpty()) {

				final AbstractToken token2 = tokens.pop();

				// example: one, two and three

				if (token2 instanceof ConjunctionToken && !tokens.isEmpty()) {

					final AbstractToken token3 = tokens.pop();

					if (token3 instanceof IdentifierToken identifier3) {

						//identifier1.addIdentifier(identifier3);

						tokens.push(new IdentifierListToken(identifier1, identifier3));

						// include syntactical tokens so we can edit the text later on
						identifier1.addSyntaxToken(token2.getToken());

					} else if (token3 instanceof ConceptToken concept1) {

						// If we arrive here, there was a known concept in the list
						// that should most likely be handled as an identifier, hence
						// we convert it back to an identifier token.
						tokens.push(new IdentifierListToken(identifier1, concept1.asIdentifierToken()));

						// include syntactical tokens so we can edit the text later on
						identifier1.addSyntaxToken(token2.getToken());

					} else {

						throw new RuntimeException("Syntax error: expected identifier in list, got " + token3 + ". Compound phrases are not supported, please use separate phrases to specify facts with different concepts.");
					}

				} else {

					// token1 is processed
					result.add(token1);

					// token2 needs to stay
					tokens.push(token2);
				}

			} else {

				result.add(token1);
			}
		}


		tokens.addAll(result);
	}
}
