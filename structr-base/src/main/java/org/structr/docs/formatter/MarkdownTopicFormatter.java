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
package org.structr.docs.formatter;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Details;
import org.structr.docs.ontology.Occurrence;

import java.util.LinkedList;
import java.util.List;

public class MarkdownTopicFormatter extends Formatter {

	@Override
	public void format(final List<String> lines, final Concept concept, final OutputSettings settings, final String link, final int level) {

		if (settings.hasDetail(Details.name) || settings.hasDetail(Details.all)) {

			lines.add(formatMarkdownHeading((link != null ? link + " " : "") + concept.getName() + " (" + concept.getType() + ")", level + 1));
			lines.add("");
		}

		if (settings.hasDetail(Details.source)) {

			final List<String> buf = new LinkedList<>();

			for (final Occurrence occurrence : concept.getOccurrences()) {

				buf.add(occurrence.getSourceFile() + ":" + occurrence.getLineNumber());
			}

			lines.add("<small>Sources: " + StringUtils.join(buf, ", ") + "</small>");
			lines.add("");
		}

		if (settings.hasDetail(Details.all)) {

			//
		}
	}
}
