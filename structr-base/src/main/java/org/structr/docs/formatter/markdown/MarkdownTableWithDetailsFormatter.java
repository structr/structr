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
package org.structr.docs.formatter.markdown;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.Documentable;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Details;
import org.structr.docs.ontology.Link;
import org.structr.docs.ontology.Verb;

import java.util.*;

public class MarkdownTableWithDetailsFormatter extends Formatter {

	@Override
	public boolean format(final List<String> lines, final Link link, final OutputSettings settings, final int level, final Set<Concept> seenConcepts) {

		final Map<String, String> headers = mapOf("Name", "`displayName`", "Description", "shortDescription", "", "details");
		final Concept concept             = link.getTarget();

		if (settings.hasDetail(Details.name)) {
			lines.add(formatMarkdownHeading(concept.getName(), level + 1));
		}

		if (settings.hasDetail(Details.shortDescription) && concept.getShortDescription() != null) {
			lines.add(concept.getShortDescription());
		}

		if (settings.hasDetail(Details.children)) {

			if (concept.getMetadata().containsKey("table-headers")) {

				final Map<String, String> tableHeaders = (Map<String, String>) concept.getMetadata().get("table-headers");
				if (!tableHeaders.isEmpty()) {

					headers.clear();
					headers.putAll(tableHeaders);
				}
			}

			// format children
			final List<Map<String, String>> documentables = new LinkedList<>();

			for (final Concept child : concept.getChildren(Verb.Has)) {

				final Documentable documentable = child.getDocumentable();
				if (documentable != null) {

					documentables.add(mapOf(
						"name", documentable.getName(),
						"displayName", documentable.getDisplayName(false),
						"shortDescription", documentable.getShortDescription(),
						"longDescription", documentable.getLongDescription(),
						"details", "<a href=\"javascript:void(0)\" class=\"open-details\" data-concept-id=\"" + child.getId() + "\">Open details</a>"
					));

				} else {

					documentables.add(mapOf(
						"name", child.getName(),
						"displayName", child.getName(),
						"shortDescription", MarkdownTableWithDetailsFormatter.coalesce(child.getShortDescription(), (String) child.getMetadata().get("description")),
						"details", ""
					));
				}
			}

			MarkdownTableWithDetailsFormatter.formatMarkdownTable(lines, headers, documentables);
		}

		return false;
	}

	public static void formatMarkdownTable(final List<String> lines, final Map<String, String> headers, final List<Map<String, String>> documentables) {

		lines.add("");
		lines.add("| " + StringUtils.join(headers.keySet(), " | ") + " |");
		lines.add("| " + StringUtils.repeat("--- | ---", headers.size() - 1) + " |");

		for (final Map<String, String> documentable : documentables) {

			final List<String> row = new LinkedList<>();

			for (final String headerName : headers.values()) {

				if (headerName.startsWith("`") && headerName.endsWith("`")) {

					final String name = headerName.substring(1, headerName.length() - 1);
					row.add("`" + documentable.get(name) + "`");

				} else {

					row.add(documentable.get(headerName));
				}
			}

			lines.add("| " + StringUtils.join(row, " | ") + " |");
		}
	}

	// ----- private methods -----
	protected static String coalesce(final String... strings) {

		for (final String string : strings) {

			if (StringUtils.isNotBlank(string)) {
				return string;
			}
		}

		return null;
	}

	protected Map<String, String> mapOf(final String... strings) {

		final Map<String, String> map = new LinkedHashMap<>();
		int len                       = strings.length;

		for (int i=0; i<len; i+=2) {

			final String key   = strings[i];
			final String value = strings[i+1];

			if (key != null && StringUtils.isNotBlank(value)) {

				map.put(key, value);
			}
		}

		return map;
	}
}
