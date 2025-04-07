/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.flow.traits.definitions.FlowContainerConfigurationTraitDefinition;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

public class FlowContainerConfiguration extends AbstractNodeTraitWrapper implements DeployableEntity {

	public FlowContainerConfiguration(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getValidForEditor() {
		return wrappedObject.getProperty(traits.key(FlowContainerConfigurationTraitDefinition.VALID_FOR_EDITOR_PROPERTY));
	}

	public String getConfigJson() {
		return wrappedObject.getProperty(traits.key(FlowContainerConfigurationTraitDefinition.CONFIG_JSON_PROPERTY));
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put(GraphObjectTraitDefinition.ID_PROPERTY,                              getUuid());
		result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                            getType());
		result.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                          getName());
		result.put(FlowContainerConfigurationTraitDefinition.VALID_FOR_EDITOR_PROPERTY, getValidForEditor());
		result.put(FlowContainerConfigurationTraitDefinition.CONFIG_JSON_PROPERTY,      getConfigJson());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,         true);
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY,  true);

		return result;
	}
}
