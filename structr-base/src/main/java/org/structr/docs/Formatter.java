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

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Link;
import org.structr.docs.ontology.Verb;

import java.util.*;

public abstract class Formatter {

	/**
	 * Formats the given annotated concept. The return value of this method determines
	 * whether the children of this concept should be rendered or not.
	 *
	 * @param lines
	 * @param link
	 * @param settings
	 * @param level
	 * @return whether the children should be rendered
	 */
	public abstract boolean format(final List<String> lines, final Link link, final OutputSettings settings, final int level, final Set<Concept> visited);

	/**
	 * Walks through the whole ontology and calls formatters for all the elements.
	 * Output generation is controlled externally by the OutputSettings instance.
	 *
	 * @param lines
	 * @param link
	 * @param outputSettings
	 * @param level
	 */
	public static void walkOntology(final List<String> lines, final Link link, final OutputSettings outputSettings, final int level, final Set<Concept> seenConcepts) {

		final Concept concept = link.getTarget();

		// max level reached, no output beyond this point
		if (level >= outputSettings.getMaxLevels()) {
			Formatter.renderComment(lines, outputSettings, concept + " not rendered because level " + level + " >= maxLevels (" + outputSettings.getMaxLevels() + ")");
			return;
		}

		if (concept == null) {
			Formatter.renderComment(lines, outputSettings, "Encountered null concept while processing ontology.");
			return;
		}

		// type filter
		if (!outputSettings.renderType(concept.getType())) {
			Formatter.renderComment(lines, outputSettings, concept + " not rendered because OutputSettings#typesToRender did not contain " + concept.getType());
			return;
		}

		// only output the same concept once
		if (!seenConcepts.add(concept)) {
			Formatter.renderComment(lines, outputSettings, concept + " not rendered because it was already rendered previously.");
			return;
		}

		String formatterType = null;
		boolean renderChildren = true;

		if (level >= outputSettings.getStartLevel()) {

			// part1: fetch formatter for concept and call format()
			final Formatter formatter = outputSettings.getFormatterForLink(link, outputSettings.getOutputMode());
			if (formatter != null) {

				renderChildren = formatter.format(lines, link, outputSettings, level, seenConcepts);

				formatterType = formatter.getClass().getSimpleName();

			} else {

				Formatter.renderComment(lines, outputSettings, concept + " not rendered because no formatter registered for format " + outputSettings.getOutputFormat() + ", mode " + outputSettings.getOutputMode() + " and " + concept);
			}
		}

		if (renderChildren) {

			// part2: recurse (children are handled externally, i.e. not inside the type-specific formatters)
			final List<Link> links = concept.getChildLinks(Verb.Has);
			boolean sortChildren   = true;

			// check if children want to be sorted
			for (final Link child : links) {
				sortChildren &= ConceptType.SortedChildren.equals(child.getFormat());
			}

			if (sortChildren) {
				Collections.sort(links, Comparator.comparing(c -> c.getTarget().getName()));
			}

			for (final Link child : links) {

				walkOntology(lines, child, outputSettings, level + 1, seenConcepts);
			}
		} else {

			Formatter.renderComment(lines, outputSettings, "Children of " + concept + " not rendered because " + formatterType + " prevents rendering of children.");
		}
	}

	// ----- protected methods -----
	protected String formatMarkdownHeading(final String text, final int level) {
		return StringUtils.repeat("#", level) + " " + text;
	}

	protected String formatListHeading(final String text, final int level) {
		return StringUtils.repeat("  ", level) + "- " + text;
	}

	protected Map<String, String> mapOf(final String... keyValue) {

		final Map<String, String> map = new LinkedHashMap<>();

		for (int i = 0; i < keyValue.length; i += 2) {

			final String key   = keyValue[i];
			final String value = keyValue[i + 1];

			map.put(key, value);
		}

		return map;
	}

	protected List<String> split(final String source) {

		final List<String> result = new LinkedList<>();

		if (source != null) {

			result.addAll(Arrays.asList(source.split("\n")));
		}

		return result;
	}

	public static void renderComment(final List<String> lines, final OutputSettings outputSettings, final String text) {

		if ("markdown".equals(outputSettings.getOutputFormat()) && outputSettings.renderComments()) {

			lines.add("<span class='info'>Markdown Rendering Hint: " + text + "</span>");

		} else {

			//System.out.println(text);
		}
	}
}
