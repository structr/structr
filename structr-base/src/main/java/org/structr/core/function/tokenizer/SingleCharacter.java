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

public class SingleCharacter extends Tokenizer {

	private boolean first = true;
	private char key = 0;

	public SingleCharacter(final char key) {
		this.key = key;
	}

	@Override
	public boolean accept(final char character) {
		return (first && key == character) || Character.isWhitespace(character);
	}

	@Override
	public String getType() {
		return Character.toString(key);
	}

	@Override
	public String getQuoteChar() {
		return null;
	}

	@Override
	public void add(final char character) {
		super.add(character);
		first = false;
	}

	@Override
	public void reset() {
		first = true;
	}

	@Override
	Tokenizer newInstance() {
		return new SingleCharacter(key);
	}
}
