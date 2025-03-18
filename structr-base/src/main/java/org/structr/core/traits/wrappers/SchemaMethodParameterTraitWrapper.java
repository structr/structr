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
package org.structr.core.traits.wrappers;

import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaMethodParameterTraitDefinition;

public class SchemaMethodParameterTraitWrapper extends AbstractNodeTraitWrapper implements SchemaMethodParameter {

	public SchemaMethodParameterTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key(SchemaMethodParameterTraitDefinition.DESCRIPTION_PROPERTY));
	}

	@Override
	public String getParameterType() {
		return wrappedObject.getProperty(traits.key(SchemaMethodParameterTraitDefinition.PARAMETER_TYPE_PROPERTY));
	}

	@Override
	public int getIndex() {
		return wrappedObject.getProperty(traits.key(SchemaMethodParameterTraitDefinition.INDEX_PROPERTY));
	}

	@Override
	public String getExampleValue() {
		return wrappedObject.getProperty(traits.key(SchemaMethodParameterTraitDefinition.EXAMPLE_VALUE_PROPERTY));
	}
}
