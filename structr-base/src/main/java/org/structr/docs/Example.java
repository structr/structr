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
	private final String description;
	private final Language language;

	public Example(final String text, final String descriptionm, final Language language) {

		this.text        = text;
		this.description = descriptionm;
		this.language    = language;
	}

	public String getText() {
		return text;
	}

	public String getDescription() {
		return description;
	}

	public Language getLanguage() {
		return language;
	}

	public static Example javaScript(final String example) {
		return new Example(example, null, Language.Javascript);
	}

	public static Example javaScript(final String example, final String description) {
		return new Example(example, description, Language.Javascript);
	}

	public static Example structrScript(final String text, final String description) {
		return new Example(text, description, Language.StructrScript);
	}

	public static Example structrScript(final String text) {
		return new Example(text, null, Language.StructrScript);
	}
}
