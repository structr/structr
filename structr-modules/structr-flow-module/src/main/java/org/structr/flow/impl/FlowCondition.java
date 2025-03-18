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

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;

/**
 *
 */
public class FlowCondition extends FlowDataSource {

	public FlowCondition(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public Iterable<FlowCondition> getConditions() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key("conditions"));

		return Iterables.map(n -> n.as(FlowCondition.class), nodes);
	}

	public void setConditions(final Iterable<FlowCondition> conditions) throws FrameworkException {
		wrappedObject.setProperty(traits.key("conditions"), conditions);
	}
}
