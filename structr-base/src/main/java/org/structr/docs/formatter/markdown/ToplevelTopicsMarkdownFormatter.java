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
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.*;
import org.structr.docs.ontology.parser.token.AbstractToken;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ToplevelTopicsMarkdownFormatter extends Formatter {

	private final Set<ConceptType> blacklistedTypes = Set.of(ConceptType.Text, ConceptType.Constant); //, ConceptType.Setting, ConceptType.Helper, ConceptType.Category, ConceptType.HttpVerb);

	@Override
	public boolean format(final List<String> lines, final Link link, final OutputSettings settings, final int level, final Set<Concept> seenConcepts) {

		final Concept concept = link.getTarget();

		// do not display blacklisted entries
		if (blacklistedTypes.contains(concept.getType())) {
			return true;
		}

		if (settings.hasDetail(Details.name) || settings.hasDetail(Details.all)) {

			// add parent topic here, but only at level 0
			if (level == 0) {

				final String parentConceptName = concept.getParentConceptName();
				if (parentConceptName != null) {

					lines.add(formatMarkdownHeading(parentConceptName, level));
				}
			}

			String title = concept.getName();

			if (concept.getMetadata().containsKey("title")) {
				title = (String) concept.getMetadata().get("title");
			}

			// this makes the name of the concept editable
			if (concept.getType().equals(ConceptType.Topic)) {

				lines.add(formatMarkdownHeading(title, level + 1));

			} else {

				lines.add(formatMarkdownHeading(title, level + 1));
			}

			lines.add("");

			final List<String> synonyms = concept.getSynonyms();
			if (!synonyms.isEmpty()) {

				lines.add("Synonyms: *" + StringUtils.join(synonyms, "*, *") + "*");
			}
		}

		if (settings.hasDetail(Details.source) || settings.hasDetail(Details.all)) {

			final List<String> buf = new LinkedList<>();

			for (final AbstractToken abstractToken : concept.getTokens()) {

				final Token token = abstractToken.getToken();
				if (token != null) {

					buf.add(token.toString());
				}
			}

			//lines.add("<span class='info'>Sources: " + StringUtils.join(buf, ", ") + "</span>");
			lines.add("");
		}

		if (settings.hasDetail(Details.shortDescription)) {

			if (concept.getShortDescription() != null) {

				lines.addAll(split(concept.getShortDescription()));

				lines.add("");
			}
		}

		return true;
	}

	private List<String> split(final String source) {

		final List<String> result = new LinkedList<>();

		if (source != null) {

			result.addAll(Arrays.asList(source.split("\n")));
		}

		return result;
	}
}
