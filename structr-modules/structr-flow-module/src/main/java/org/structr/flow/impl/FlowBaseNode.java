/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.flow.traits.definitions.FlowBaseNodeTraitDefinition;
import org.structr.flow.traits.operations.GetExportData;
import org.structr.module.api.DeployableEntity;

import java.util.Map;

public class FlowBaseNode extends AbstractNodeTraitWrapper implements DeployableEntity {

	public FlowBaseNode(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final FlowDataSource getDataSource() {

		final NodeInterface dataSource = wrappedObject.getProperty(traits.key(FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY));
		if (dataSource != null) {

			return dataSource.as(FlowDataSource.class);
		}

		return null;
	}

	public final FlowContainer getFlowContainer() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(FlowBaseNodeTraitDefinition.FLOW_CONTAINER_PROPERTY));
		if (node != null) {

			return node.as(FlowContainer.class);
		}

		return null;
	}

	public final void setDataSource(final FlowDataSource dataSource) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY), dataSource);
	}

	public final void setFlowContainer(final FlowContainer flowContainer) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowBaseNodeTraitDefinition.FLOW_CONTAINER_PROPERTY), flowContainer);
	}

	public Map<String, Object> exportData() {
		return traits.getMethod(GetExportData.class).getExportData(this);
	}
}
