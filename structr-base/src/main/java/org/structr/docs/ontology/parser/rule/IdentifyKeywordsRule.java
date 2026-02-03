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
import org.structr.docs.ontology.parser.token.*;

import java.util.Deque;
import java.util.LinkedList;

public class IdentifyKeywordsRule extends Rule {

	public IdentifyKeywordsRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(Deque<AbstractToken> tokens) {

		final Deque<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken token1 = tokens.poll();

			if (token1 instanceof UnresolvedToken unresolvedToken && unresolvedToken.getToken() != null) {

				final Token name = unresolvedToken.getToken();

				if ("with".equals(name.toLowerCase())) {

					result.add(new WithToken(name));

				}  else if ("as".equals(name.toLowerCase())) {

					result.add(new AsToken(name));

				}  else if ("new".equals(name.toLowerCase())) {

					result.add(new NewKeywordToken(name));

				} else {

					result.add(token1);
				}

			} else {

				result.add(token1);
			}
		}

		tokens.addAll(result);
	}
}
