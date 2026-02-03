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
import org.structr.docs.ontology.parser.token.AbstractToken;
import org.structr.docs.ontology.parser.token.NewlineToken;
import org.structr.docs.ontology.parser.token.UnresolvedToken;

import java.util.Deque;
import java.util.LinkedList;

public class IdentifyNewlinesRule extends Rule {

	public IdentifyNewlinesRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(Deque<AbstractToken> tokens) {

		final Deque<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken abstractToken = tokens.poll();

			if (abstractToken instanceof UnresolvedToken unresolved && "\n".equals(unresolved.getToken().getContent())) {

				result.add(new NewlineToken(unresolved.getToken()));

			} else {

				result.add(abstractToken);
			}
		}

		tokens.addAll(result);
	}
}
