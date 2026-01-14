/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.impl;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.traits.definitions.FlowKeyValueTraitDefinition;
import org.structr.flow.traits.definitions.FlowObjectDataSourceTraitDefinition;

public class FlowObjectDataSource extends FlowDataSource {

	public FlowObjectDataSource(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final Iterable<FlowKeyValue> getKeyValueSources() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(FlowObjectDataSourceTraitDefinition.KEY_VALUE_SOURCES_PROPERTY));

		return Iterables.map(n -> n.as(FlowKeyValue.class), nodes);
	}

	public void setKeyValueSources(final Iterable<FlowKeyValue> keyValueSources) throws FrameworkException {

		wrappedObject.setProperty(traits.key(FlowObjectDataSourceTraitDefinition.KEY_VALUE_SOURCES_PROPERTY), keyValueSources);
	}
}
