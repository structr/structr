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
import org.structr.docs.ontology.parser.token.AnaphoricPronounToken;
import org.structr.docs.ontology.parser.token.AbstractToken;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class IdentifyAnaphoricPronounRule extends Rule {

	public IdentifyAnaphoricPronounRule(final Ontology ontology) {
		super(ontology);
	}

	@Override
	public void apply(final Deque<AbstractToken> tokens) {

		final List<AbstractToken> result = new LinkedList<>();

		while (!tokens.isEmpty()) {

			final AbstractToken token = tokens.pop();

			if (token.isUnresolved() && "it".equals(token.getToken().toLowerCase())) {

				result.add(new AnaphoricPronounToken(token.getToken()));

			} else {

				result.add(token);
			}
		}




		tokens.addAll(result);
	}
}
