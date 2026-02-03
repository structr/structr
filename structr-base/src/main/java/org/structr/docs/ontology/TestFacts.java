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
package org.structr.docs.ontology;

import org.structr.core.function.tokenizer.FactsTokenizer;
import org.structr.core.function.tokenizer.Token;

import java.util.LinkedList;
import java.util.List;

public class TestFacts extends FactsContainer {

	private final List<Token> tokens = new LinkedList<>();
	private final String facts;
	private final String name;

	public TestFacts(final String name, final String facts) {

		this.name = name;
		this.facts = facts;

		initialize();
	}

	@Override
	public void writeToDisc() {
	}

	@Override
	public List<Token> getTokens() {
		return tokens;
	}

	@Override
	public String getName() {
		return name;
	}

	// ----- private methods -----
	private void initialize() {

		final FactsTokenizer factsTokenizer = new  FactsTokenizer();

		tokens.addAll(factsTokenizer.tokenize(this, facts));
	}
}
