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
package org.structr.docs.formatter.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.docs.Documentable;
import org.structr.docs.ontology.*;
import org.structr.docs.ontology.Concept;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SearchResultsConceptFormatter {

	public void format(final List<String> lines, final Concept concept, final Double score) {

		final Gson gson                = new GsonBuilder().create();
		final Map<String, Object> data = new LinkedHashMap<>();

		data.put("id",          concept.getId());
		data.put("name",        concept.getName());
		data.put("type",        concept.getType());
		data.put("score",       score);

		final Documentable documentable = concept.getDocumentable();
		if (documentable != null) {

			data.put("shortDescription", documentable.getShortDescription());
		}

		data.putAll(concept.getMetadata());

		collectParents(concept, data, 0);

		lines.add(gson.toJson(data));
	}

	// ----- private methods -----
	private void collectParents(final Concept concept, final Map<String, Object> data, final int level) {

		if (level > 3) {
			return;
		}

		final List<Map<String, Object>> parents = new LinkedList<>();

		data.put("parents", parents);

		for (final Map.Entry<Verb, List<Link>> parent : concept.getParentLinks().entrySet()) {

			final List<Map<String, Object>> targets = new LinkedList<>();

			for (final Link parentLink : parent.getValue()) {

				final Concept parentConcept        = parentLink.getSource();
				final Map<String, Object> childMap = new LinkedHashMap<>();

				childMap.put("id",         parentConcept.getId());
				childMap.put("name",       parentConcept.getName());
				childMap.put("type",       parentConcept.getType());
				childMap.put("isToplevel", parentConcept.isToplevelConcept());

				collectParents(parentConcept, childMap, level + 1);

				targets.add(childMap);
			}

			parents.add(Map.of(
				"name", parent.getKey(),
				"targets", targets
			));
		}
	}
}
