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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.Filter;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowConditionBaseNode;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlowFilter extends FlowNode implements DataSource, Filter, DeployableEntity {

	private static final Logger logger                              = LoggerFactory.getLogger(FlowFilter.class);
	public static final Property<DataSource> dataSource             = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<Iterable<FlowBaseNode>> dataTarget = new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<FlowCondition> condition           = new StartNode<>("condition", FlowConditionBaseNode.class);

	public static final View defaultView = new View(FlowDataSource.class, PropertyView.Public, dataTarget, dataSource, condition);
	public static final View uiView      = new View(FlowDataSource.class, PropertyView.Ui, dataTarget, dataSource, condition);

	public FlowFilter(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Object get(Context context) throws FlowException {
		Object data = context.getData(getUuid());
		if (data == null) {
			filter(context);
			data = context.getData(getUuid());
		}
		return data;
	}

	@Override
	public void filter(Context context) throws FlowException {

		DataSource ds = getProperty(dataSource);
		FlowCondition condition = getProperty(FlowFilter.condition);

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

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());

		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

}
