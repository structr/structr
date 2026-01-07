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
package org.structr.core.function.tokenizer;

public class Token {

	private final String type;
	private final String content;
	private final String quote;
	private final int row;
	private final int column;

	public Token(final String type, final String content, final String quote, final int row, final int column) {

		this.type    = type;
		this.content = content;
		this.row     = row;
		this.column  = column;
		this.quote   = quote;
	}

	@Override
	public String toString() {
		return content;
	}

	public String getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public String getQuote() {
		return quote;
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}
}
