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
package org.structr.core.entity;

import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.parser.*;

import java.util.List;
import java.util.Set;

public interface SchemaProperty extends PropertyDefinition, NodeInterface {

	AbstractSchemaNode getSchemaNode();
	String getSourceContentType();
	String getStaticSchemaNodeName();

	String getNotionBaseProperty();
	Set<String> getPropertiesForNotionProperty();
	String getNotionMultiplicity();

	NotionPropertyGenerator getNotionPropertyParser();
	IntegerPropertyGenerator getIntPropertyParser();
	LongPropertyGenerator getLongPropertyParser();
	DoublePropertyGenerator getDoublePropertyParser();

	PropertyKey createKey(final String className) throws FrameworkException;
	List<IsValid> createValidators(final AbstractSchemaNode entity) throws FrameworkException;

	/**
	 * The name of the relationship property for one end of a relationship. An explicitly configured json
	 * name is taken as it is - deriving a name is only for the ends that have none - but it still passes
	 * through the uniqueness handling below, so an explicit name that collides also gets a suffix.
	 *
	 * Delegates to {@link SchemaRelationshipNode}, which owns this naming scheme.
	 */
	static String getPropertyName(final Set<String> existingPropertyNames, final boolean outgoing, final String relationshipTypeName, final String _sourceType, final String _targetType, final String _targetJsonName, final String _targetMultiplicity, final String _sourceJsonName, final String _sourceMultiplicity) {

		return SchemaRelationshipNode.getPropertyName(existingPropertyNames, outgoing, relationshipTypeName, _sourceType, _targetType, _targetJsonName, _targetMultiplicity, _sourceJsonName, _sourceMultiplicity);
	}
}
