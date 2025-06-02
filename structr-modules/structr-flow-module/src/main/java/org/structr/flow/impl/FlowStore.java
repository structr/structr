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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.traits.definitions.FlowStoreTraitDefinition;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

public class FlowStore extends FlowDataSource implements DeployableEntity {

	private static final Logger logger = LoggerFactory.getLogger(FlowStore.class);

	public FlowStore(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public enum Operation {
		store,
		retrieve
	}

	public final String getOperation() {
		return wrappedObject.getProperty(traits.key(FlowStoreTraitDefinition.OPERATION_PROPERTY));
	}

	public final String getKey() {
		return wrappedObject.getProperty(traits.key(FlowStoreTraitDefinition.KEY_PROPERTY));
	}

	public final void handleStorage(final Context context) throws FlowException {

		// FIXME: Operation is EnumProperty - update code accordingly
		final String op         = getOperation();
		final String _key       = getKey();
		final FlowDataSource ds = getDataSource();

		if(op != null && _key != null ) {

			switch (op) {
				case "store":
					if (ds != null) {
						context.putIntoStore(_key, ds.get(context));
					}
					break;
				case "retrieve":
					context.setData(getUuid(), context.retrieveFromStore(_key));
					break;
			}

		} else {

			logger.warn("Unable to handle FlowStore{}, missing operation or key.", getUuid());
		}
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             getUuid());
		result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           getType());
		result.put(FlowStoreTraitDefinition.KEY_PROPERTY,                              getKey());
		result.put(FlowStoreTraitDefinition.OPERATION_PROPERTY,                        getOperation());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        isVisibleToPublicUsers());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, isVisibleToAuthenticatedUsers());

		return result;
	}

}
