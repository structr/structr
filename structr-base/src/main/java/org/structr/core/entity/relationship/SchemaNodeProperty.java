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
package org.structr.core.entity.relationship;

import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaProperty;

/**
 *
 *
 */
public class SchemaNodeProperty extends OneToMany<AbstractSchemaNode, SchemaProperty> {

	@Override
	public Class<AbstractSchemaNode> getSourceType() {
		return AbstractSchemaNode.class;
	}

	@Override
	public Class<SchemaProperty> getTargetType() {
		return SchemaProperty.class;
	}

	@Override
	public String name() {
		return "HAS_PROPERTY";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}

	@Override
	public boolean isInternal() {
		return true;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}