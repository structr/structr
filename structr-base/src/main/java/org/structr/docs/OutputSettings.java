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
package org.structr.docs;

import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Details;

import java.util.*;

public class OutputSettings {

	private final Map<String, Map<Concept.Type, Formatter>> outputFormatTypeFormatterMap = new LinkedHashMap<>();
	private final Map<Integer, Set<String>> linkTypesPerLevel                            = new LinkedHashMap<>();
	private final Set<Concept.Type> typesToRender                                        = new LinkedHashSet<>();
	private final Set<Details> details                                                   = new LinkedHashSet<>();
	private String outputFormat                                                          = "text";
	private String baseUrl;
	private int startLevel;
	private int maxLevels;

	public OutputSettings(final int startLevel, final int maxLevels) {

		this.startLevel = startLevel;
		this.maxLevels = maxLevels;
	}

	/**
	 * Returns the formatter for the given type and the output format
	 * configured in this settings object, or null if no formatter was
	 * registered for the given combination.
	 *
	 * @param type
	 * @return
	 */
	public Formatter getFormatterForType(final Concept.Type type) {

		final Map<Concept.Type, Formatter> formatters = outputFormatTypeFormatterMap.get(outputFormat);
		if (formatters != null) {

			final Formatter formatter = formatters.get(type);
			if (formatter != null) {

				return formatter;
			}

			final Formatter defaultFormatter = formatters.get(Concept.Type.Unknown);
			if (defaultFormatter != null) {

				return defaultFormatter;
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

	public Set<Details> getDetails() {
		return details;
	}

	public boolean hasDetail(final Details detail) {
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

	public void setFormatterForOutputFormatAndType(final String outputFormat, final Concept.Type type, final Formatter formatter) {
		outputFormatTypeFormatterMap.computeIfAbsent(outputFormat, k -> new LinkedHashMap<>()).put(type, formatter);
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

	public void setTypesToRender(final Set<Concept.Type> typeList) {

		typesToRender.clear();
		typesToRender.addAll(typeList);
	}

	public boolean renderType(final Concept.Type type) {
		return typesToRender.isEmpty() || typesToRender.contains(type);
	}
}
