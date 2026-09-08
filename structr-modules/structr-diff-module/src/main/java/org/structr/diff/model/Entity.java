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
package org.structr.diff.model;

import java.util.Map;
import java.util.TreeMap;

/**
 * One thing in an export, whatever kind of thing it is.
 *
 * There is a single entity shape rather than a class per kind, because the matcher and the diff
 * have no business knowing what a Page is. What varies per kind is the KEY, and that variation is
 * decided in the parser and carried here as data.
 *
 * <p>Two keys, and the order between them is the design:
 *
 * <ul>
 * <li>{@link #getKey()} is the primary identity, the uuid wherever the export carries one.
 *     Measured across two real instances, uuids survive at 2347 of 2348 DOM elements, 169 of 169
 *     page-level containers and 783 of 783 localizations, so this is almost always sufficient, and
 *     it is what makes a rename expressible at all.</li>
 * <li>{@link #getAlternateKey()} is a weaker, kind-specific fallback: a signature, a name, a path.
 *     It exists for nodes a seeder created independently on each instance, whose uuids therefore
 *     cannot agree. It is tried ONLY on entities the primary key failed to match, because a weaker
 *     key must never overrule a uuid that matched perfectly well.</li>
 * </ul>
 *
 * <p>That two-tier arrangement replaced a first attempt which keyed the seeded kinds by name and
 * ignored the uuid outright. It was wrong in a way only measurement showed: three ResourceAccess
 * grants share the signature {@code DOMElement/_id/event}, and they carry IDENTICAL uuids on both
 * instances because a user made them. Ignoring the uuid there threw away good identity and turned
 * one unambiguous match into three ambiguous ones.
 */
public class Entity {

	private final String kind;
	private final String key;
	private final String alternateKey;
	private final String parent;
	private final String name;
	private final Map<String, Object> attributes;
	private final String content;
	private final Integer ordinal;
	private final String origin;

	public Entity(final String kind, final String key, final String alternateKey, final String parent,
			final String name, final Map<String, Object> attributes, final String content,
			final Integer ordinal, final String origin) {

		this.kind         = kind;
		this.key          = key;
		this.alternateKey = alternateKey;
		this.parent       = parent;
		this.name         = name;
		this.attributes   = attributes != null ? attributes : new TreeMap<>();
		this.content      = content;
		this.ordinal      = ordinal;
		this.origin       = origin;
	}

	public String getKind() {

		return kind;
	}

	public String getKey() {

		return key;
	}

	public String getAlternateKey() {

		return alternateKey;
	}

	public String getParent() {

		return parent;
	}

	public String getName() {

		return name;
	}

	public Map<String, Object> getAttributes() {

		return attributes;
	}

	public String getContent() {

		return content;
	}

	public Integer getOrdinal() {

		return ordinal;
	}

	/** The export file this came from. Reported to a reader, never used to match. */
	public String getOrigin() {

		return origin;
	}

	/** Identity within an export: kind plus primary key. */
	public String getIdentity() {

		return kind + " " + key;
	}

	/** Fallback identity, or null when this kind has no fallback. */
	public String getAlternateIdentity() {

		return alternateKey != null ? kind + " " + alternateKey : null;
	}

	@Override
	public String toString() {

		return kind + " " + (name != null ? name : key);
	}
}
