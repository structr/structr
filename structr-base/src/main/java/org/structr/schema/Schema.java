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
package org.structr.schema;

import org.structr.api.graph.PropertyContainer;
import org.structr.common.PropertyView;
import org.structr.core.entity.*;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public interface Schema {

	// views in this set will be serialized as (id, type, name) if nested
	public static final Set<String> RestrictedViews = Set.of(PropertyView.All, PropertyView.Ui, PropertyView.Custom);

	public String getMultiplicity(final Map<String, SchemaNode> schemaNodes, final String propertyNameToCheck);
	public String getRelatedType(final Map<String, SchemaNode> schemaNodes, final String propertyNameToCheck);
	public PropertyContainer getPropertyContainer();
	public String getClassName();
	public String getSuperclassName();
	public String getUuid();

	public Iterable<SchemaProperty> getSchemaProperties();
	public Iterable<SchemaView> getSchemaViews();
	public Iterable<SchemaMethod> getSchemaMethods();
	public Iterable<SchemaMethod> getSchemaMethodsIncludingInheritance();
	public Iterable<SchemaGrant> getSchemaGrants();
}
