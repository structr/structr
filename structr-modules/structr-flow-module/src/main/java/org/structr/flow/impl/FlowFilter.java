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
import org.structr.api.util.Iterables;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.traits.definitions.FlowFilterTraitDefinition;
import org.structr.module.api.DeployableEntity;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlowFilter extends FlowDataSource implements DeployableEntity {

	private static final Logger logger = LoggerFactory.getLogger(FlowFilter.class);

	public FlowFilter(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final FlowCondition getCondition() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(FlowFilterTraitDefinition.CONDITION_PROPERTY));
		if (node != null) {

			return node.as(FlowCondition.class);
		}

		return null;
	}

	public void filter(final Context context) throws FlowException {

		final FlowDataSource ds       = getDataSource();
		final FlowCondition condition = getCondition();

		if (ds != null) {

			Object data = ds.get(context);

			if (data instanceof Iterable) {

				if (condition != null) {

					data = Iterables.toList((Iterable) data).stream().filter(el -> {

						try {
							context.setData(getUuid(), el);
							return (Boolean)condition.get(context);

						} catch (FlowException ex) {
							logger.warn("Exception in FlowFilter filter(): " + ex.getMessage());
						}

						return false;
					});

					data = ((Stream) data).collect(Collectors.toList());

					context.setData(getUuid(), data);

				} else {

					context.setData(getUuid(), data);
				}
			}
		}
	}
}
