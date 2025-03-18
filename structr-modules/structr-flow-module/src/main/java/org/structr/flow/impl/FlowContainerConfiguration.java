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
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

public class FlowContainerConfiguration extends AbstractNodeTraitWrapper implements DeployableEntity {

	public FlowContainerConfiguration(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public boolean getValidForEditor() {
		return wrappedObject.getProperty(traits.key("validForEditor"));
	}

	public String getConfigJson() {
		return wrappedObject.getProperty(traits.key("configJson"));
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("name",                        getName());
		result.put("validForEditor",              getValidForEditor());
		result.put("configJson",                  getConfigJson());
		result.put("visibleToPublicUsers",        true);
		result.put("visibleToAuthenticatedUsers", true);

		return result;
	}
}
