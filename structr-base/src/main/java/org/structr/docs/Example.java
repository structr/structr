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
package org.structr.docs;

public class Example {

	private final String text;
	private final String title;
	private final Language language;

	public Example(final String text, final String title, final Language language) {

		this.text        = text;
		this.title = title;
		this.language    = language;
	}

	public String getText() {
		return text;
	}

	public String getTitle() {
		return title;
	}

	public Language getLanguage() {
		return language;
	}

	public static Example javaScript(final String example) {
		return new Example(example, null, Language.JavaScript);
	}

	public static Example javaScript(final String example, final String title) {
		return new Example(example, title, Language.JavaScript);
	}

	public static Example structrScript(final String text, final String title) {
		return new Example(text, title, Language.StructrScript);
	}

	public static Example structrScript(final String text) {
		return new Example(text, null, Language.StructrScript);
	}

	public static Example html(final String text, final String title) {
		return new Example(text, title, Language.Html);
	}

	public static Example html(final String text) {
		return new Example(text, null, Language.Html);
	}
}
