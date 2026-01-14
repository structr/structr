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
package org.structr.docs;

import java.util.List;

public class Usage {

	private final String usage;
	private final List<Language> languages;

	public Usage(final String description, final Language... languages) {

		this.usage     = description;
		this.languages = List.of(languages);
	}

	public String getUsage() {
		return usage;
	}

	public List<Language> getLanguages() {
		return languages;
	}

	public static Usage javaScript(final String usage) {
		return new Usage(usage, Language.JavaScript);
	}

	public static Usage structrScript(final String usage) {
		return new Usage(usage, Language.StructrScript);
	}

	public boolean isJavaScript() {
		return languages.contains(Language.JavaScript);
	}

	public boolean isStructrScript() {
		return languages.contains(Language.StructrScript);
	}
}
