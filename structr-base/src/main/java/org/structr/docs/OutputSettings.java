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

import org.structr.docs.ontology.*;

import java.util.*;

public class OutputSettings {

	private final Map<String, Map<String, Map<ConceptType, Formatter>>> formatterMap = new LinkedHashMap<>();
	private final Map<Integer, Set<String>> linkTypesPerLevel                        = new LinkedHashMap<>();
	private final Set<ConceptType> typesToRender                                     = new LinkedHashSet<>();
	private final Set<Details> details                                               = new LinkedHashSet<>(Set.of(Details.all));
	private boolean renderComments                                                   = true;
	private String outputMode                                                        = "overview";
	private String outputFormat                                                      = "markdown";
	private int levelOffset                                                          = 0;
	private final Ontology ontology;
	private String baseUrl;
	private String key;
	private int startLevel;
	private int maxLevels;

	public OutputSettings(final Ontology ontology, final int startLevel, final int maxLevels) {

		this.ontology   = ontology;
		this.startLevel = startLevel;
		this.maxLevels  = maxLevels;
	}

	/**
	 * Returns the formatter for the given concept and output format
	 * configured in this settings object, or null if no formatter was
	 * registered for the given combination.
	 *
	 * @param link
	 * @return
	 */
	public Formatter getFormatterForLink(final Link link, final String mode) {

		final Concept concept = link.getTarget();

		final Map<String, Map<ConceptType, Formatter>> formatters = formatterMap.get(outputFormat);
		if (formatters != null) {

			final Map<ConceptType, Formatter> modeMap = formatters.get(mode);
			if (modeMap != null) {

				// format wins
				if (link.getFormatSpecification() != null) {

					final ConceptType format  = link.getFormatSpecification().getFormat();
					final Formatter formatter = modeMap.get(format);
					if (formatter != null) {

						return formatter;
					}
				}

				// then type
				final Formatter formatter = modeMap.get(concept.getType());
				if (formatter != null) {

					return formatter;
				}

				// then default
				final Formatter defaultFormatter = modeMap.get(ConceptType.Unknown);
				if (defaultFormatter != null) {

					return defaultFormatter;
				}
			}
		}

		return null;
	}

	public Set<String> getLinkTypesToFollow(final int level) {

		final Set<String> linkTypes = linkTypesPerLevel.get(level);
		if (linkTypes != null) {

			return linkTypes;
		}

		// return a default?
		return null;
	}

	public String getOutputMode() {
		return outputMode;
	}

	public Set<Details> getDetails() {
		return details;
	}

	public boolean hasDetail(final Details detail) {

		if (details.contains(Details.all)) {
			return true;
		}

		return details.contains(detail);
	}

	public int getStartLevel() {
		return startLevel;
	}

	public int getMaxLevels() {
		return startLevel + maxLevels;
	}

	public boolean isLinkTypeEnabled(final int level, final String linkKey) {

		final Set<String> linkTypes = getLinkTypesToFollow(level);
		if (linkTypes != null) {

			return linkTypes.contains(linkKey);
		}

		return false;
	}

	public void setLinkTypes(Map<Integer, Set<String>> linkTypes) {

		this.linkTypesPerLevel.clear();
		this.linkTypesPerLevel.putAll(linkTypes);
	}

	public void setStartLevel(final int level) {
		this.startLevel = level;
	}

	public void setFormatterForOutputFormatModeAndType(final String outputFormat, final String mode, final ConceptType type, final Formatter formatter) {
		formatterMap.computeIfAbsent(outputFormat, k -> new LinkedHashMap<>()).computeIfAbsent(mode, k -> new LinkedHashMap<>()).put(type, formatter);
	}

	public void setBaseUrl(final String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setMaxLevels(final int maxLevels) {
		this.maxLevels = maxLevels;
	}

	public void setOutputFormat(final String format) {
		this.outputFormat = format;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setTypesToRender(final Set<ConceptType> typeList) {

		typesToRender.clear();
		typesToRender.addAll(typeList);
	}

	public boolean renderType(final ConceptType type) {
		return typesToRender.isEmpty() || typesToRender.contains(type);
	}

	public Ontology getOntology() {
		return ontology;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

	public static OutputSettings withDetails(final Ontology ontology, final Details... details) {

		final OutputSettings settings = new OutputSettings(ontology, 0, 6);

		settings.getDetails().clear();

		for (final Details detail : details) {
			settings.getDetails().add(detail);
		}

		return settings;
	}

	public boolean renderComments() {
		return renderComments;
	}

	public void setRenderComments(final boolean value) {
		this.renderComments = value;
	}

	public void setLevelOffset(final int levelOffset) {
		this.levelOffset = levelOffset;
	}

	public int getLevelOffset() {
		return levelOffset;
	}
}
