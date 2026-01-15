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
import org.structr.docs.ontology.parser.token.ConceptToken;
import org.structr.docs.ontology.parser.token.NewKeywordToken;
import org.structr.docs.ontology.parser.token.AbstractToken;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class ResolveKeywordsRule extends Rule {

	public ResolveKeywordsRule(Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final List<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken token1 = tokens.pop();

			if (token1 instanceof NewKeywordToken && !tokens.isEmpty()) {

				AbstractToken token2 = tokens.pop();

				if (token2 instanceof ConceptToken concept2) {

					concept2.setAllowReuse(false);

					result.add(concept2);

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
