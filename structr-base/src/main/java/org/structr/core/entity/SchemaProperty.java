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
package org.structr.core.entity;

import org.structr.core.app.Query;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;
import org.structr.schema.parser.*;

import java.util.Map;
import java.util.Set;

public interface SchemaProperty extends PropertyDefinition, NodeTrait {

	NodeInterface getSchemaNode();
	String getSourceContentType();

	String getNotionBaseProperty(final Map<String, NodeInterface> schemaNodes);
	Set<String> getPropertiesForNotionProperty(final Map<String, NodeInterface> schemaNodes);
	String getNotionMultiplicity(final Map<String, NodeInterface> schemaNodes);

	NotionPropertyParser getNotionPropertyParser(final Map<String, NodeInterface> schemaNodes);
	IntPropertyParser getIntPropertyParser(final Map<String, NodeInterface> schemaNodes);
	IntegerArrayPropertyParser getIntArrayPropertyParser(final Map<String, NodeInterface> schemaNodes);
	LongPropertyParser getLongPropertyParser(final Map<String, NodeInterface> schemaNodes);
	LongArrayPropertyParser getLongArrayPropertyParser(final Map<String, NodeInterface> schemaNodes);
	DoublePropertyParser getDoublePropertyParser(final Map<String, NodeInterface> schemaNodes);
	DoubleArrayPropertyParser getDoubleArrayPropertyParser(final Map<String, NodeInterface> schemaNodes);
}
