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
package org.structr.core.function.tokenizer;

import org.structr.docs.ontology.FactsContainer;

public abstract class Tokenizer {

	private StringBuilder buf = new StringBuilder();
	private int row = 0;
	private int column = 0;

	abstract boolean accept(final char character);

	abstract Tokenizer newInstance();

	abstract String getQuoteChar();

	abstract String getType();

	@Override
	public String toString() {
		return getType();
	}


	public void add(final char character) {
		buf.append(character);
	}

	public String getContent() {
		return buf.toString();
	}

	public Token getToken(final FactsContainer factsContainer) {
		return new Token(factsContainer, getType(), getContent(), getQuoteChar(), row, column);
	}

	public void init(final int row, final int column) {
		this.column = column;
		this.row    = row;
	}

	public void reset() {
		buf.setLength(0);
	}
}
