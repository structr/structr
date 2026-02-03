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
import org.structr.docs.ontology.Verb;
import org.structr.docs.ontology.parser.token.AbstractToken;
import org.structr.docs.ontology.parser.token.UnresolvedToken;
import org.structr.docs.ontology.parser.token.VerbToken;

import java.util.*;

public class IdentifyVerbsRule extends Rule {

	public IdentifyVerbsRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final Deque<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken abstractToken = tokens.pop();

			if (abstractToken instanceof UnresolvedToken unresolved) {

				final Token token  = unresolved.getToken();
				final String name  = token.toLowerCase();
				final Verb verb    = Verb.leftToRight(name);
				final Verb inverse = Verb.rightToLeft(name);

				if (verb != null) {

					result.add(new VerbToken(token, verb, false));

				} else if (inverse != null) {

					result.add(new VerbToken(token, inverse, true));

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
