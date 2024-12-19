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

import org.structr.common.helper.CaseHelper;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;
import org.structr.schema.parser.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface SchemaProperty extends PropertyDefinition, NodeTrait {

	AbstractSchemaNode getSchemaNode();
	String getSourceContentType();

	String getNotionBaseProperty(final Map<String, SchemaNode> schemaNodes);
	Set<String> getPropertiesForNotionProperty(final Map<String, SchemaNode> schemaNodes);
	String getNotionMultiplicity(final Map<String, SchemaNode> schemaNodes);

	Set<String> getEnumDefinitions();

	NotionPropertyParser getNotionPropertyParser(final Map<String, SchemaNode> schemaNodes);
	IntPropertyParser getIntPropertyParser(final Map<String, SchemaNode> schemaNodes);
	IntegerArrayPropertyParser getIntArrayPropertyParser(final Map<String, SchemaNode> schemaNodes);
	LongPropertyParser getLongPropertyParser(final Map<String, SchemaNode> schemaNodes);
	LongArrayPropertyParser getLongArrayPropertyParser(final Map<String, SchemaNode> schemaNodes);
	DoublePropertyParser getDoublePropertyParser(final Map<String, SchemaNode> schemaNodes);
	DoubleArrayPropertyParser getDoubleArrayPropertyParser(final Map<String, SchemaNode> schemaNodes);

	static String getPropertyName(final String relatedClassName, final Set<String> existingPropertyNames, final boolean outgoing, final String relationshipTypeName, final String _sourceType, final String _targetType, final String _targetJsonName, final String _targetMultiplicity, final String _sourceJsonName, final String _sourceMultiplicity) {

		String propertyName = "";

		if (outgoing) {


			if (_targetJsonName != null) {

				// FIXME: no automatic creation?
				propertyName = _targetJsonName;

			} else {

				if ("1".equals(_targetMultiplicity)) {

					propertyName = CaseHelper.toLowerCamelCase(relationshipTypeName) + CaseHelper.toUpperCamelCase(_targetType);

				} else {

					propertyName = CaseHelper.plural(CaseHelper.toLowerCamelCase(relationshipTypeName) + CaseHelper.toUpperCamelCase(_targetType));
				}
			}

		} else {


			if (_sourceJsonName != null) {
				propertyName = _sourceJsonName;
			} else {

				if ("1".equals(_sourceMultiplicity)) {

					propertyName = CaseHelper.toLowerCamelCase(_sourceType) + CaseHelper.toUpperCamelCase(relationshipTypeName);

				} else {

					propertyName = CaseHelper.plural(CaseHelper.toLowerCamelCase(_sourceType) + CaseHelper.toUpperCamelCase(relationshipTypeName));
				}
			}
		}

		if (existingPropertyNames.contains(propertyName)) {

			// First level: Add direction suffix
			propertyName += outgoing ? "Out" : "In";
			int i = 0;

			// New name still exists: Add number
			while (existingPropertyNames.contains(propertyName)) {
				propertyName += ++i;
			}

		}

		existingPropertyNames.add(propertyName);

		return propertyName;
	}
}
