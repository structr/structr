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
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.*;
import org.structr.docs.ontology.Concept;

import java.util.*;

public class TableOfContentsConceptFormatter extends Formatter {

	@Override
	public boolean format(final List<String> lines, final Link link, final OutputSettings settings, final int level, final Set<Concept> seenConcepts) {

		final Concept concept                 = link.getTarget();
		final Gson gson                       = new GsonBuilder().create();
		final List<Map<String, Object>> links = new LinkedList<>();
		final Map<String, Object> data        = new LinkedHashMap<>();

		data.put("id",    concept.getId());
		data.put("name",  concept.getName());
		data.put("type",  concept.getType());
		data.put("links", links);

		for (final Map.Entry<Verb, List<Link>> child : concept.getChildLinks().entrySet()) {

			final List<Map<String, Object>> childList = new LinkedList<>();
			final Verb verb                          = child.getKey();

			if (Verb.Has.equals(verb)) {

				for (final Link childLink : child.getValue()) {

					final Concept childConcept         = childLink.getTarget();
					final Map<String, Object> childMap = new LinkedHashMap<>();

					childMap.put("id",   childConcept.getId());
					childMap.put("name", childConcept.getName());
					childMap.put("type", childConcept.getType());

					childList.add(childMap);
				}

				links.add(Map.of(
					"name", child.getKey(),
					"targets", childList
				));
			}
		}

		lines.add(gson.toJson(data));

		return false;
	}
}
