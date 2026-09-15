/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.diff.compare;

import org.structr.diff.model.Entity;
import org.structr.diff.model.Kind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * What an export contains, in the terms its author would recognise: the domain model and the pages.
 */
public class Profile {

	private static final int SAMPLE = 8;

	public static Map<String, Object> of(final List<Entity> entities) {

		final List<Entity> types      = entities.stream().filter(e -> Kind.SCHEMA_TYPE.equals(e.getKind())).toList();
		final List<Entity> domain     = types.stream().filter(e -> !isRelationship(e)).toList();
		final Map<String, Integer> propertiesPerType = propertiesPerType(entities);
		final List<String> topTypes = domain.stream()
			.map(Entity::getName)
			.filter(n -> n != null)
			.sorted(Comparator
				.comparingInt((String n) -> propertiesPerType.getOrDefault(n, 0)).reversed()
				.thenComparing(Comparator.naturalOrder()))
			.limit(SAMPLE)
			.collect(Collectors.toList());

		final List<String> pageNames = entities.stream()
			.filter(e -> Kind.PAGE.equals(e.getKind()))
			.map(Entity::getName)
			.filter(n -> n != null)
			.sorted()
			.toList();

		final Set<String> locales = new TreeSet<>();

		for (final Entity e : entities) {

			if (Kind.LOCALIZATION.equals(e.getKind()) && e.getAttributes() != null) {

				final Object locale = e.getAttributes().get("locale");
				if (locale != null) {

					locales.add(unquote(locale.toString()));
				}
			}
		}

		final Map<String, Object> profile = new LinkedHashMap<>();

		profile.put("pages", pageNames.size());
		profile.put("domainTypes", domain.size());
		profile.put("relationshipTypes", types.size() - domain.size());
		profile.put("properties", count(entities, Kind.SCHEMA_PROPERTY));
		profile.put("methods", count(entities, Kind.SCHEMA_METHOD) + count(entities, Kind.GLOBAL_METHOD));
		profile.put("files", count(entities, Kind.FILE));
		profile.put("locales", new ArrayList<>(locales));
		profile.put("topTypes", topTypes);
		profile.put("samplePages", pageNames.stream().limit(SAMPLE).collect(Collectors.toList()));
		profile.put("description", describe(pageNames.size(), domain.size(), types.size() - domain.size(), count(entities, Kind.SCHEMA_PROPERTY), topTypes, pageNames, locales));

		return profile;
	}

	/**
	 * A sentence assembled from what was counted, never inferred: the reader should be able to check
	 * every clause against the numbers beside it.
	 */
	private static String describe(final int pages, final int domain, final int relationships,
			final int properties, final List<String> topTypes, final List<String> pageNames, final Set<String> locales) {

		final StringBuilder text = new StringBuilder();

		text.append(domain).append(domain == 1 ? " domain type" : " domain types");
		text.append(" joined by ").append(relationships).append(relationships == 1 ? " relationship" : " relationships");
		text.append(", ").append(properties).append(" properties");
		text.append(", across ").append(pages).append(pages == 1 ? " page" : " pages");

		if (!locales.isEmpty()) {

			text.append(", localised into ").append(String.join(", ", locales));
		}

		text.append(".");

		if (!topTypes.isEmpty()) {

			text.append(" The richest types are ").append(String.join(", ", topTypes)).append(".");
		}

		if (!pageNames.isEmpty()) {

			text.append(" Pages include ").append(String.join(", ", pageNames.subList(0, Math.min(SAMPLE, pageNames.size())))).append(".");
		}

		return text.toString();
	}

	/** Attributes hold the raw JSON text so that comparison stays faithful, so a string still has quotes. */
	private static String unquote(final String value) {

		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {

			return value.substring(1, value.length() - 1);
		}

		return value;
	}

	/** A relationship type carries its ends; a node type carries properties and traits. */
	private static boolean isRelationship(final Entity type) {

		final Map<String, Object> attributes = type.getAttributes();

		return attributes != null && (attributes.containsKey("$source") || attributes.containsKey("rel"));
	}

	private static Map<String, Integer> propertiesPerType(final List<Entity> entities) {

		final Map<String, Integer> counts = new HashMap<>();

		for (final Entity e : entities) {

			if (Kind.SCHEMA_PROPERTY.equals(e.getKind()) && e.getParent() != null) {

				counts.merge(e.getParent(), 1, Integer::sum);
			}
		}

		return counts;
	}

	private static int count(final List<Entity> entities, final String kind) {

		return (int) entities.stream().filter(e -> kind.equals(e.getKind())).count();
	}

	private Profile() {}
}
