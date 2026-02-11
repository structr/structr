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

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.ontology.FactsContainer;

import java.util.List;

public class Token {

	private final FactsContainer source;
	private final String type;
	private final String quote;
	private final int row;
	private final int column;
	private String content;

	public Token(final FactsContainer sourceFile, final String type, final String content, final String quote, final int row, final int column) {

		this.source  = sourceFile;
		this.type    = type;
		this.content = content;
		this.row     = row;
		this.column  = column;
		this.quote   = quote;
	}

	@Override
	public String toString() {
		return source.getName() + ":" + row + ":" + column;
	}

	public String getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public String getRawContent() {

		if (quote != null) {

			return quote + content + quote;
		}

		return getContent();
	}

	public Token copy(final String newContent) {
		return new Token(source, type, newContent, quote, row, column);
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

	public String getSource() {
		return source.getName();
	}

	public boolean startsWith(final String prefix) {
		return content.startsWith(prefix);
	}

	public boolean endsWith(final String suffix) {
		return content.endsWith(suffix);
	}

	public String toLowerCase() {
		return content.toLowerCase();
	}

	public Character charAt(int i) {
		return content.charAt(i);
	}

	public boolean isNotBlank() {
		return StringUtils.isNotBlank(content) || content.equals("\n");
	}

	public boolean isComment() {
		return "comment".equals(type);
	}

	public void setContent(String newContent) {
		this.content = newContent;
	}

	public List<Token> insertAfter(final String text) {
		return source.insertAfter(this, text);
	}

	public void remove() {
		source.remove(this);
	}

	public boolean isCapitalized() {

		if (StringUtils.isNotBlank(content)) {

			final char c = content.charAt(0);

			return Character.isUpperCase(c);
		}

		return false;
	}
}
