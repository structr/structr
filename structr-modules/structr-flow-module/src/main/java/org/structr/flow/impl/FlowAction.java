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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.traits.definitions.FlowActionTraitDefinition;
import org.structr.flow.traits.operations.ActionOperations;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

public class FlowAction extends FlowDataSource implements DeployableEntity, ThrowingElement {

	public FlowAction(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final String getScript() {
		return wrappedObject.getProperty(traits.key(FlowActionTraitDefinition.SCRIPT_PROPERTY));
	}

	public final void setScript(final String script) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowActionTraitDefinition.SCRIPT_PROPERTY), script);
	}

	public final void execute(final Context context) throws FlowException {
		traits.getMethod(ActionOperations.class).execute(context, this);
	}

	@Override
	public Map<String, Object> exportData() {
		
		final Map<String, Object> result = new TreeMap<>();

		result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             getUuid());
		result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           getType());
		result.put(FlowActionTraitDefinition.SCRIPT_PROPERTY,                          getScript());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        isVisibleToPublicUsers());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, isVisibleToAuthenticatedUsers());

		return result;
	}

}
