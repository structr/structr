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
package org.structr.api.schema;

import org.structr.api.graph.Cardinality;

import java.net.URI;

/**
 *
 *
 */
public interface JsonObjectType extends JsonType {

	JsonReferenceType relate(final JsonObjectType type);
	JsonReferenceType relate(final URI externalTypeReference);
	JsonReferenceType relate(final JsonObjectType type, final String relationship);
	JsonReferenceType relate(final URI externalTypeReference, final String relationship);
	JsonReferenceType relate(final JsonObjectType type, final String relationship, final Cardinality cardinality);
	JsonReferenceType relate(final URI externalTypeReference, final String relationship, final Cardinality cardinality);
	JsonReferenceType relate(final JsonObjectType type, final String relationship, final Cardinality cardinality, final String sourceAttributeName, final String targetAttributeName);
	JsonReferenceType relate(final URI externalTypeReference, final String relationship, final Cardinality cardinality, final String sourceAttributeName, final String targetAttributeName);
	JsonReferenceType relate(final Class type, final String relationship, final Cardinality cardinality, final String sourceAttributeName, final String targetAttributeName);
}
