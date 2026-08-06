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

public class MapTokenizer extends Tokenizer {

	private boolean inSingleQuotes = false;
	private boolean inDoubleQuotes = false;
	private boolean inIdentifier   = false;
	private int level              = 0;

	public MapTokenizer() {
	}

	@Override
	public boolean accept(final char character) {

		if (level == 0) {

			return character == '{';
		}

		// accept everything

		return true;
	}

	@Override
	public void add(final char character) {

		switch (character) {

			case '\'':
				inSingleQuotes = !inSingleQuotes;
				break;

			case '"':
				inDoubleQuotes = !inDoubleQuotes;
				break;

			case '{':

				if (!inSingleQuotes && !inDoubleQuotes) {

					level++;
				}

				break;

			case '}':

				if (!inSingleQuotes && !inDoubleQuotes) {

					level--;
				}

				break;

		}

		super.add(character);
	}

	@Override
	Tokenizer newInstance() {

		return new MapTokenizer();
	}

	@Override
	String getQuoteChar() {

		return "";
	}

	@Override
	String getType() {

		return "map";
	}
}
