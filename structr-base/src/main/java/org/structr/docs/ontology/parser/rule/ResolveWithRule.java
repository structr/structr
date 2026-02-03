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

public class ResolveWithRule extends Rule {

	public ResolveWithRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final Deque<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken token1 = tokens.pop();

			if (token1 instanceof NamedConceptToken concept1 && !tokens.isEmpty()) {

				// unresolved => check if next is existing concept
				final AbstractToken token2 = tokens.pop();

				if (token2 instanceof WithToken preposition && !tokens.isEmpty()) {

					final AbstractToken token3 = tokens.pop();

					if (token3 instanceof NamedConceptToken concept2) {

						concept1.addAdditionalNamedConcept(concept2);

						result.add(token1);

					} else {

						result.add(token1);

						// reset tokens 2 & 3 to be evaluated again
						tokens.push(token2);
						tokens.push(token3);
					}

				} else {

					result.add(token1);

					// reset token2 to be evaluated again
					tokens.push(token2);
				}

			} else {

				result.add(token1);
			}
		}

		// restore input
		tokens.addAll(result);
	}
}
