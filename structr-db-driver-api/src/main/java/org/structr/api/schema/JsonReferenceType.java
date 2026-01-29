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
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.api.schema.JsonSchema.Cascade;

import java.net.URI;

/**
 *
 *
 */
public interface JsonReferenceType extends JsonType {

	JsonReferenceType setRelationship(final String relationship);
	String getRelationship();

	JsonReferenceType setCardinality(final Cardinality cardinality);
	Cardinality getCardinality();

	Cascade getCascadingDelete();
	Cascade getCascadingCreate();

	JsonReferenceType setCascadingDelete(final Cascade cascade);
	JsonReferenceType setCascadingCreate(final Cascade cascade);

	PropagationDirection getPermissionPropagation();
	PropagationMode getReadPermissionPropagation();
	PropagationMode getWritePermissionPropagation();
	PropagationMode getDeletePermissionPropagation();
	PropagationMode getAccessControlPermissionPropagation();

	JsonReferenceType setPermissionPropagation(final PropagationDirection value);
	JsonReferenceType setReadPermissionPropagation(final PropagationMode value);
	JsonReferenceType setWritePermissionPropagation(final PropagationMode value);
	JsonReferenceType setDeletePermissionPropagation(final PropagationMode value);
	JsonReferenceType setAccessControlPermissionPropagation(final PropagationMode value);

	URI getSourceType();
	URI getTargetType();

	String getSourcePropertyName();
	String getTargetPropertyName();

	JsonReferenceType setSourcePropertyName(final String sourcePropertyName);
	JsonReferenceType setTargetPropertyName(final String targetPropertyName);

	JsonReferenceProperty getSourceProperty();
	JsonReferenceProperty getTargetProperty();
}
