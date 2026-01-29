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

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.traits.definitions.FlowParameterInputTraitDefinition;
import org.structr.module.api.DeployableEntity;

public class FlowParameterInput extends FlowBaseNode implements DeployableEntity {

	public FlowParameterInput(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getKey() {
		return wrappedObject.getProperty(traits.key(FlowParameterInputTraitDefinition.KEY_PROPERTY));
	}

	public void process(final Context context, final Context functionContext) throws FlowException {

		final FlowDataSource _ds = getDataSource();
		final String _key        = getKey();

		if(_ds != null && _key != null) {

			Object data = _ds.get(context);
			functionContext.setParameter(_key, data);
		}
	}
}
