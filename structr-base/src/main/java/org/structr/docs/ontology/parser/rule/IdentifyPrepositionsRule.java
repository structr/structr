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
import org.structr.docs.ontology.parser.token.PrepositionToken;
import org.structr.docs.ontology.parser.token.Token;

import java.util.Deque;
import java.util.LinkedList;

public class IdentifyPrepositionsRule extends Rule {

	public IdentifyPrepositionsRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(Deque<Token> tokens) {

		final Deque<Token> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final Token token1 = tokens.poll();
			final String name  = token1.getName();

			if (token1.isUnresolved() && name != null && "with".equals(name.toLowerCase())) {

				result.add(new PrepositionToken(name));

			} else {

				result.add(token1);
			}
		}

		tokens.addAll(result);
	}
}
