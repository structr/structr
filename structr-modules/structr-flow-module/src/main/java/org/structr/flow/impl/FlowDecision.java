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
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class FlowDecision extends FlowNode implements DeployableEntity {

	public FlowDecision(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public FlowLogicCondition getCondition() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("condition"));
		if (node != null) {

			return node.as(FlowLogicCondition.class);
		}

		return null;
	}

	public void setCondition(final FlowDataSource condition) throws FrameworkException {
		wrappedObject.setProperty(traits.key("condition"), condition);
	}

	public FlowNode getTrueElement() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("trueElement"));
		if (node != null) {

			return node.as(FlowNode.class);
		}

		return null;
	}

	public FlowNode getFalseElement() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("falseElement"));
		if (node != null) {

			return node.as(FlowNode.class);
		}

		return null;
	}

	public void setTrueElement(final FlowNode trueElement) throws FrameworkException {
		wrappedObject.setProperty(traits.key("trueElement"), trueElement);
	}

	public void setFalseElement(final FlowNode falseElement) throws FrameworkException {
		wrappedObject.setProperty(traits.key("falseElement"), falseElement);
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}
}
