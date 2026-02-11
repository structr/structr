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
import org.structr.docs.ontology.parser.token.AbstractToken;
import org.structr.docs.ontology.parser.token.UnresolvedToken;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.UnaryOperator;

public class RemoveUnwantedTokensRule extends Rule {

	public RemoveUnwantedTokensRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final Set<String> blacklist = ontology.getBlacklist();
		final Deque<AbstractToken> result   = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken abstractToken = tokens.poll();

			if (abstractToken instanceof UnresolvedToken unresolved && blacklist.contains(unresolved.getToken().toLowerCase())) {

				// ignore token

				/*
			} else {

				final Token token = abstractToken.getToken();
				final String name = token.getContent();

				if (name.endsWith(".")) {
					abstractToken.setToken(token);
				}

				result.add(abstractToken);
				*/

			} else {
				result.add(abstractToken);
			}
		}

		tokens.addAll(result);
	}
}
