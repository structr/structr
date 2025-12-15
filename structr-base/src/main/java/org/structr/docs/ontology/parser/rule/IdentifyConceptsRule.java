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

import org.structr.docs.ontology.Ontology;
import org.structr.docs.ontology.parser.token.BlacklistToken;
import org.structr.docs.ontology.parser.token.ConceptToken;
import org.structr.docs.ontology.parser.token.Token;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

public class IdentifyConceptsRule extends Rule {

	public IdentifyConceptsRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<Token> tokens) {

		final Set<String> knownConcepts = ontology.getKnownConcepts();
		final Deque<Token> result       = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final Token token = tokens.pop();

			// quoted tokens can never be keywords
			if (token.isUnresolved() && !token.isInQuotes()) {

				final String name                  = token.getName();
				final String lowercaseName         = name.toLowerCase();
				final String singularLowercaseName = lowercaseName.substring(0, lowercaseName.length() - 1);
				final String singularName          = name.substring(0, lowercaseName.length() - 1);

				if ("blacklist".equals(lowercaseName)) {

					result.add(new BlacklistToken(name));

				} else if (knownConcepts.contains(lowercaseName)) {

					// use original case for concept token
					result.add(new ConceptToken(lowercaseName, name));

				} else if (knownConcepts.contains(singularLowercaseName)) {

					// our "language" has only simple plurals with "s" at the end!
					// use original case for concept token, but singular
					result.add(new ConceptToken(singularName, name));

				} else {

					result.add(token);
				}

			} else {

				// move to result
				result.add(token);
			}
		}

		// restore input
		tokens.addAll(result);
	}
}
