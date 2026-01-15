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

public class ResolveIsARelationsRule extends Rule {

	public ResolveIsARelationsRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final Deque<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken token1 = tokens.poll();

			if (token1 instanceof NamedConceptToken name1 && name1.isUnknown() && !tokens.isEmpty()) {

				final AbstractToken token2 = tokens.poll();

				if (token2 instanceof VerbToken verbToken && "is".equals(verbToken.getToken()) && !tokens.isEmpty()) {

					final AbstractToken token3 = tokens.poll();

					if (token3 instanceof ConceptToken conceptToken) {

						result.add(new IsAToken(name1, conceptToken));

					} else {

						// no match => move tokens back
						result.add(token1);
						result.add(token2);
						result.add(token3);
					}

				} else {

					// no match => move tokens back
					result.add(token1);
					result.add(token2);
				}

			} else {

				// no match => move tokens back
				result.add(token1);
			}
		}

		tokens.addAll(result);

	}
}
