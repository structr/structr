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
import org.structr.docs.DocumentableType;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Details;
import org.structr.docs.ontology.Link;
import org.structr.docs.ontology.Verb;

import java.util.*;

public class MarkdownTableFormatter extends Formatter {

	@Override
	public boolean format(final List<String> lines, final Link link, final OutputSettings settings, final int level, final Set<Concept> seenConcepts) {

		final Map<String, String> headers = mapOf("Name", "`displayName`", "Description", "shortDescription");
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

			lines.add("");
			lines.add("| " + StringUtils.join(headers.keySet(), " | ") + " |");
			lines.add("| " + StringUtils.repeat("--- | ---", headers.size() - 1) + " |");

			// format children
			final List<Concept> children = concept.getChildren(Verb.Has);
			if (children != null) {

				final List<Documentable> documentables = new LinkedList<>();

				for (final Concept child : children) {

					// mark concept as visited so it is not rendered again
					seenConcepts.add(child);

					final Documentable documentable = child.getDocumentable();
					if (documentable != null) {

						documentables.add(documentable);

					} else {

						documentables.add(new Documentable() {
							@Override
							public DocumentableType getDocumentableType() {
								return DocumentableType.Constant;
							}

							@Override
							public String getName() {
								return child.getName();
							}

							@Override
							public String getShortDescription() {
								return coalesce(child.getShortDescription(), (String) child.getMetadata().get("description"));
							}
						});
					}
				}

				Collections.sort(documentables, Comparator.comparing(documentable1 -> documentable1.getDisplayName()));

				for (final Documentable documentable : documentables) {

					final List<String> row = new LinkedList<>();

					for (final String value : headers.values()) {

						switch (value) {

							case "`name`":
								row.add("`" + documentable.getName() + "`");
								break;

							case "name":
								row.add(documentable.getName());
								break;

							case "`displayName`":
								row.add("`" + documentable.getDisplayName(false) + "`");
								break;

							case "displayName":
								row.add(documentable.getDisplayName(false));
								break;

							case "shortDescription":
								row.add(documentable.getShortDescription());
								break;
						}
					}

					lines.add("| " + StringUtils.join(row, " | ") + " |");
				}
			}
		}

		return false;
	}

	// ----- private methods -----
	private String coalesce(final String... strings) {

		for (final String string : strings) {

			if (StringUtils.isNotBlank(string)) {
				return string;
			}
		}

		return null;
	}
}
