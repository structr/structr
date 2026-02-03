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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GroupedDocumentationEntry extends DocumentationEntry {

	private final Map<String, List<String>> linesPerCategory = new TreeMap<>();
	private final String titleSuffix;

	public GroupedDocumentationEntry(final String fileName, final String header, final String titleSuffix) {

		super(fileName, header);

		this.titleSuffix = titleSuffix;
	}

	@Override
	public void addLines(final List<String> lines, final String... additionalInfo) {

		final String category = additionalInfo[0];

		List<String> list = linesPerCategory.get(category);
		if (list == null) {

			list = new LinkedList<>();
			linesPerCategory.put(category, list);

			if (titleSuffix != null && !category.endsWith(titleSuffix)) {

				list.add("## " + category + " " + titleSuffix);

			} else {

				list.add("## " + category);
			}

			list.add("");
		}

		list.addAll(lines);
	}

	@Override
	public List<String> getLines() {

		final List<String> result = new LinkedList<>(lines);

		for (final List<String> lines : linesPerCategory.values()) {
			result.addAll(lines);
		}

		return result;
	}
}
