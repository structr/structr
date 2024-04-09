/*
 * Copyright (C) 2010-2024 Structr GmbH
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

	public JsonReferenceType setRelationship(final String relationship);
	public String getRelationship();

	public JsonReferenceType setCardinality(final Cardinality cardinality);
	public Cardinality getCardinality();

	public Cascade getCascadingDelete();
	public Cascade getCascadingCreate();

	public JsonReferenceType setCascadingDelete(final Cascade cascade);
	public JsonReferenceType setCascadingCreate(final Cascade cascade);

	public PropagationDirection getPermissionPropagation();
	public PropagationMode getReadPermissionPropagation();
	public PropagationMode getWritePermissionPropagation();
	public PropagationMode getDeletePermissionPropagation();
	public PropagationMode getAccessControlPermissionPropagation();

	public JsonReferenceType setPermissionPropagation(final PropagationDirection value);
	public JsonReferenceType setReadPermissionPropagation(final PropagationMode value);
	public JsonReferenceType setWritePermissionPropagation(final PropagationMode value);
	public JsonReferenceType setDeletePermissionPropagation(final PropagationMode value);
	public JsonReferenceType setAccessControlPermissionPropagation(final PropagationMode value);

	public URI getSourceType();
	public URI getTargetType();

	public String getSourcePropertyName();
	public String getTargetPropertyName();

	public JsonReferenceType setSourcePropertyName(final String sourcePropertyName);
	public JsonReferenceType setTargetPropertyName(final String targetPropertyName);

	public JsonReferenceProperty getSourceProperty();
	public JsonReferenceProperty getTargetProperty();


}
