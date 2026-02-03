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

public class Identifier extends Tokenizer {

	private int index = 0;

	@Override
	public boolean accept(final char character) {

		if (index == 0) {

			return Character.isAlphabetic(character) || character == '_' || character == '.';

		} else {

			return Character.isAlphabetic(character) || Character.isDigit(character) || character == '_' || character == '.' || character == '!' || character == '-';
		}
	}

	@Override
	public void add(final char character) {
		super.add(character);
		index++;
	}

	@Override
	public String getType() {
		return "identifier";
	}

	@Override
	public String getQuoteChar() {
		return null;
	}

	@Override
	Tokenizer newInstance() {
		return new Identifier();
	}
}
