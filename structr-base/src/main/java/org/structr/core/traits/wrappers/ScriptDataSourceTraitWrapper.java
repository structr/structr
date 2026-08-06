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
package org.structr.core.traits.wrappers;

import org.structr.core.GraphObject;
import org.structr.core.entity.ScriptDataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.ScriptDataSourceTraitDefinition;

public class ScriptDataSourceTraitWrapper extends DataSourceTraitWrapper implements ScriptDataSource<GraphObject> {

	public ScriptDataSourceTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	@Override
	public String getValuesScript() {

		return wrappedObject.getProperty(traits.key(ScriptDataSourceTraitDefinition.VALUES_SCRIPT_PROPERTY));
	}

	@Override
	public String getFieldsScript() {

		return wrappedObject.getProperty(traits.key(ScriptDataSourceTraitDefinition.FIELDS_SCRIPT_PROPERTY));
	}

	@Override
	public String getDataType() {

		return wrappedObject.getProperty(traits.key(ScriptDataSourceTraitDefinition.DATA_TYPE_PROPERTY));
	}
}
