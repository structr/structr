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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JsonConceptFormatter extends Formatter {

	@Override
	public void format(final List<String> lines, final Concept concept, final OutputSettings settings, final String link, final int level) {

		final StringBuilder sb = new StringBuilder();
		final List<String> c   = new LinkedList<>();
		int linkCount          = concept.getTotalChildCount();

		for (final Map.Entry<String, List<Concept>> child : concept.getChildren().entrySet()) {

			final List<String> t = new LinkedList<>();

			for (final Concept childConcept : child.getValue()) {

				t.add("{ \"name\": \"" + childConcept.getName() + "\", \"type\": \"" + childConcept.getType() + "\" }");
			}

			c.add("{ \"name\": \"" + child.getKey() + "\", \"targets\": [ " + StringUtils.join(t, ", ") + " ] }");
		}

		sb.append("{ \"name\": \"");
		sb.append(concept.getName());
		sb.append("\", ");
		sb.append("\"type\": \"");
		sb.append(concept.getType());
		sb.append("\", ");
		sb.append("\"count\": ");
		sb.append(linkCount);
		sb.append(", \"links\": [");

		sb.append(StringUtils.join(c, ", "));
		sb.append("] }");

		lines.add(sb.toString());

	}

}
