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

import org.structr.core.function.tokenizer.Token;
import org.structr.docs.ontology.Ontology;
import org.structr.docs.ontology.parser.token.ConceptToken;
import org.structr.docs.ontology.parser.token.IdentifierToken;
import org.structr.docs.ontology.parser.token.AbstractToken;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class ResolveConceptPairsRule extends Rule {

	public ResolveConceptPairsRule(Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final List<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken token1 = tokens.pop();

			if (token1 instanceof ConceptToken concept1 && !tokens.isEmpty()) {

				AbstractToken token2 = tokens.pop();

				if (token2 instanceof ConceptToken concept2) {

					// two concepts, check case first
					final Token originalToken1 = concept1.getOriginalToken();
					final Token originalToken2 = concept2.getOriginalToken();

					if (!originalToken1.isCapitalized() || !originalToken2.isCapitalized()) {

						final Character c1 = originalToken1.charAt(0);
						final Character c2 = originalToken2.charAt(0);

						// uppercase is identifier, lowercase is concept
						if (Character.isUpperCase(c1) && !Character.isUpperCase(c2)) {

							result.add(new IdentifierToken(originalToken1));
							result.add(concept2);

						} else if (!Character.isUpperCase(c1) && Character.isUpperCase(c2)) {

							result.add(concept1);
							result.add(new IdentifierToken(originalToken2));

						} else {

							// both lower case => no change
							result.add(concept1);
							result.add(concept2);
						}

					} else {

						// both capitalized and probably not concepts but identifiers!
						result.add(concept1.asIdentifierToken());
						result.add(concept2.asIdentifierToken());
					}

				} else {

					// token1 has been processed
					result.add(token1);

					// token2 needs to go back in input
					tokens.push(token2);
				}

			} else {

				result.add(token1);
			}
		}

		tokens.addAll(result);
	}
}
