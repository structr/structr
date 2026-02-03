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
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;
import org.structr.docs.ontology.parser.token.BlacklistToken;
import org.structr.docs.ontology.parser.token.ConceptToken;
import org.structr.docs.ontology.parser.token.AbstractToken;
import org.structr.docs.ontology.parser.token.UnresolvedToken;

import java.util.Deque;
import java.util.LinkedList;

public class IdentifyConceptsRule extends Rule {

	public IdentifyConceptsRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final Deque<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken abstractToken = tokens.pop();

			// quoted tokens can never be keywords
			if (abstractToken instanceof UnresolvedToken unresolved && !unresolved.isInQuotes()) {

				final Token token                  = unresolved.getToken();
				final String name                  = token.getContent();
				final String lowercaseName         = name.toLowerCase();
				final String singularLowercaseName = lowercaseName.substring(0, lowercaseName.length() - 1);
				final String singularName          = name.substring(0, lowercaseName.length() - 1);

				if ("blacklist".equals(lowercaseName)) {

					result.add(new BlacklistToken(token));

				} else if (Concept.exists(lowercaseName)) {

					// use original case for concept token
					result.add(new ConceptToken(Concept.forName(lowercaseName), token));

				} else if (Concept.exists(singularLowercaseName)) {

					// our "language" has only simple plurals with "s" at the end!
					// use original case for concept token, but singular
					result.add(new ConceptToken(Concept.forName(singularLowercaseName), token));

				} else {

					result.add(abstractToken);
				}

			} else {

				// move to result
				result.add(abstractToken);
			}
		}

		// restore input
		tokens.addAll(result);
	}
}
